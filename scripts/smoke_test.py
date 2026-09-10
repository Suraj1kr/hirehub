"""Run the packaged application and verify the main workflow over real HTTP."""
import http.cookiejar
import json
import os
from pathlib import Path
import secrets
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request

BASE = "http://localhost:18080"


class Client:
    def __init__(self):
        self.http = urllib.request.build_opener(
            urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar())
        )

    def request(self, method, path, payload=None, form=False):
        headers = {}
        if method != "GET":
            csrf = self.request("GET", "/api/csrf")
            headers[csrf["headerName"]] = csrf["token"]
        data = None
        if payload is not None:
            data = (
                urllib.parse.urlencode(payload) if form else json.dumps(payload)
            ).encode()
            headers["Content-Type"] = (
                "application/x-www-form-urlencoded" if form else "application/json"
            )
        request = urllib.request.Request(BASE + path, data, headers, method=method)
        with self.http.open(request, timeout=10) as response:
            content = response.read().decode()
            return json.loads(content) if "application/json" in response.headers.get(
                "Content-Type", ""
            ) else content


def main():
    password = secrets.token_urlsafe(24)
    environment = {
        **os.environ,
        "ADMIN_EMAIL": "admin@smoke.test",
        "ADMIN_PASSWORD": password,
    }
    log_path = Path("target/smoke-app.log")
    with log_path.open("w") as log:
        process = subprocess.Popen(
            ["java", "-jar", "target/hirehub-1.0.0.jar",
             "--spring.profiles.active=demo", "--server.port=18080"],
            stdout=log, stderr=subprocess.STDOUT, env=environment,
        )
        try:
            admin, candidate = Client(), Client()
            for attempt in range(60):
                if process.poll() is not None:
                    raise RuntimeError("Application exited during startup")
                try:
                    admin.request("GET", "/api/csrf")
                    break
                except (urllib.error.URLError, TimeoutError):
                    time.sleep(1)
            else:
                raise RuntimeError("Application did not become ready")

            admin.request("POST", "/login", {
                "username": "admin@smoke.test", "password": password
            }, form=True)
            assert admin.request("GET", "/api/me")["role"] == "ADMIN"
            job = admin.request("POST", "/api/admin/jobs", {
                "title": "Java Developer", "company": "Smoke Test Company",
                "location": "Remote", "description": "Build reliable services.",
                "active": True,
            })
            candidate.request("POST", "/api/auth/register", {
                "name": "Candidate", "email": "candidate@smoke.test",
                "password": password,
            })
            candidate.request("POST", "/login", {
                "username": "candidate@smoke.test", "password": password
            }, form=True)
            application = candidate.request(
                "POST", f"/api/applications/job/{job['id']}",
                {"coverLetter": "I build Java services and write integration tests."},
            )
            assert "Java Developer" in candidate.request("GET", "/applications")
            assert "candidate@smoke.test" in admin.request("GET", "/admin/applications")
            admin.request("PATCH", f"/api/admin/applications/{application['id']}/status",
                          {"status": "ACCEPTED"})
            mine = candidate.request("GET", "/api/applications")
            assert mine["content"][0]["status"] == "ACCEPTED"
            candidate.request("POST", "/logout")
            try:
                candidate.request("GET", "/api/me")
                raise AssertionError("Session remained authenticated after logout")
            except urllib.error.HTTPError as error:
                assert error.code == 401
            print("HTTP smoke test passed: admin login, job creation, registration, "
                  "user login, application, rendered pages, review, and logout.")
        except Exception:
            print(log_path.read_text(errors="replace"))
            raise
        finally:
            process.terminate()
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()


if __name__ == "__main__":
    main()
