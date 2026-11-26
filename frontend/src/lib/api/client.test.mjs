import assert from "node:assert/strict";
import { afterEach, test } from "node:test";
import { api, ApiError, getErrorMessage } from "./client.ts";

const originalFetch = globalThis.fetch;

afterEach(() => {
  globalThis.fetch = originalFetch;
});

function json(data, status = 200, headers = {}) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json", ...headers },
  });
}

test("writes read fresh CSRF after session rotation and support empty responses", async () => {
  const calls = [];
  const responses = [
    json({ headerName: "X-CSRF-TOKEN", token: "before-login" }),
    new Response(null, { status: 204 }),
    json({ headerName: "X-CSRF-TOKEN", token: "after-login" }),
    json({ submission_id: 42 }, 202),
  ];
  globalThis.fetch = async (url, options) => {
    calls.push({ url, options });
    return responses.shift();
  };

  assert.equal(
    await api.post("/auth/login", {
      email: "learner@example.test",
      password: "a secret phrase",
    }),
    undefined,
  );
  assert.deepEqual(
    await api.post("/exercises/7/submissions", {
      language: "python",
      source_code: "print(1)",
    }),
    { submission_id: 42 },
  );
  assert.deepEqual(
    calls.map((call) => call.url),
    [
      "/api/v1/auth/csrf",
      "/api/v1/auth/login",
      "/api/v1/auth/csrf",
      "/api/v1/exercises/7/submissions",
    ],
  );
  assert.equal(calls[1].options.headers.get("X-CSRF-TOKEN"), "before-login");
  assert.equal(calls[3].options.headers.get("X-CSRF-TOKEN"), "after-login");
  assert.equal(calls[0].options.headers.has("Content-Type"), false);
  assert.ok(calls.every((call) => call.options.credentials === "include"));
  assert.equal(
    calls[3].options.headers.get("Content-Type"),
    "application/json",
  );
});

test("failed writes preserve ProblemDetail correlation and are never replayed", async () => {
  let requests = 0;
  globalThis.fetch = async () => {
    requests += 1;
    if (requests === 1)
      return json({ headerName: "X-CSRF-TOKEN", token: "fresh" });
    return json({ code: "RATE_LIMITED", traceId: "request-42" }, 429);
  };

  await assert.rejects(
    api.post("/exercises/7/submissions", {
      language: "python",
      source_code: "print(1)",
    }),
    (error) => {
      assert.ok(error instanceof ApiError);
      assert.equal(error.status, 429);
      assert.equal(error.code, "RATE_LIMITED");
      assert.equal(error.traceId, "request-42");
      assert.match(getErrorMessage(error), /chờ/);
      return true;
    },
  );
  assert.equal(requests, 2);
});

test("reverse proxy failures stay actionable without exposing server diagnostics", async () => {
  globalThis.fetch = async () =>
    new Response("<html>Internal proxy diagnostics</html>", {
      status: 502,
      headers: { "X-Request-Id": "proxy-42" },
    });

  await assert.rejects(api.get("/courses"), (error) => {
    assert.equal(error.code, "HTTP_502");
    assert.equal(error.traceId, "proxy-42");
    assert.equal(
      getErrorMessage(error, "Vui lòng thử lại."),
      "Vui lòng thử lại.",
    );
    return true;
  });
  assert.equal(
    getErrorMessage(
      new ApiError(500, "INTERNAL_ERROR", { detail: "Database credentials" }),
      "Thử lại",
    ),
    "Thử lại",
  );
});

test("cancelled queries retain their abort reason", async () => {
  const controller = new AbortController();
  globalThis.fetch = async (_url, { signal }) =>
    new Promise((_resolve, reject) => {
      signal.addEventListener("abort", () => reject(signal.reason), {
        once: true,
      });
    });

  const pending = api.get("/courses", { signal: controller.signal });
  controller.abort(
    new DOMException("Navigation cancelled this request", "AbortError"),
  );
  await assert.rejects(pending, (error) => error === controller.signal.reason);
});
