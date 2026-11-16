// CodeForge - Google sign-in (shared by login.html and signup.html)
//
// Flow (Google Identity Services, popup):
//   1. User clicks the Google button -> Google popup -> we receive a Google ACCESS TOKEN
//   2. We send it to our backend: POST /api/auth/google  { accessToken }
//   3. Backend verifies it with Google, finds or creates the user, and returns the same
//      AuthResponse as /auth/login: { token, type, id, username, email, fullName, role }
//
// NOTE: Google sign-in does not work when the page is opened as file://.
//       Serve the frontend over http://localhost (e.g. VS Code Live Server).
window.CodeForgeGoogle = (function () {
  'use strict';

  // ===== Config =====
  // TODO: create an OAuth Client ID (type "Web application") in Google Cloud Console and paste it here.
  // Add your frontend origin (e.g. http://localhost:5500) to "Authorized JavaScript origins".
  const GOOGLE_CLIENT_ID = '501667220540-vm6ti043u9li4jkql91t7idl5st58tvb.apps.googleusercontent.com';

  // TODO backend: POST { accessToken } -> AuthResponse
  const ENDPOINT = 'http://localhost:8080/api/auth/google';

  // true  = backend/Google not set up yet: signIn() rejects with code 'MOCK' (UI preview only)
  // false = run the real flow. Set to false once GOOGLE_CLIENT_ID and the backend endpoint exist.
  const MOCK_PENDING = false;

  const GSI_SRC = 'https://accounts.google.com/gsi/client';
  let gsiPromise = null;

  function codeError(code, status) {
    const err = new Error(code);
    err.code = code;
    if (status) err.status = status;
    return err;
  }

  // Load Google's script. Call preload() on page load so the popup can open instantly
  // (browsers block popups that are opened after an async wait).
  function preload() {
    if (MOCK_PENDING) return Promise.resolve();
    if (window.google && google.accounts && google.accounts.oauth2) return Promise.resolve();
    if (!gsiPromise) {
      gsiPromise = new Promise((resolve, reject) => {
        const s = document.createElement('script');
        s.src = GSI_SRC;
        s.async = true;
        s.onload = resolve;
        s.onerror = () => { gsiPromise = null; reject(codeError('LOAD_FAILED')); };
        document.head.appendChild(s);
      });
    }
    return gsiPromise;
  }

  function requestAccessToken() {
    return new Promise((resolve, reject) => {
      const client = google.accounts.oauth2.initTokenClient({
        client_id: GOOGLE_CLIENT_ID,
        scope: 'openid email profile',
        callback: (res) => {
          if (res.error) reject(codeError(res.error === 'access_denied' ? 'CANCELLED' : 'GOOGLE_ERROR'));
          else resolve(res.access_token);
        },
        error_callback: (e) => {
          reject(codeError(e && e.type === 'popup_closed' ? 'CANCELLED' : 'GOOGLE_ERROR'));
        }
      });
      client.requestAccessToken();
    });
  }

  async function exchangeWithBackend(accessToken) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 10000);
    try {
      const res = await fetch(ENDPOINT, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify({ accessToken }),
        signal: controller.signal
      });
      let body = null;
      try { body = await res.json(); } catch (_) { /* empty / non-JSON body */ }
      if (!res.ok) throw codeError('BACKEND_ERROR', res.status);
      if (!body || !body.token) throw codeError('BAD_RESPONSE');
      return body;
    } finally {
      clearTimeout(timeout);
    }
  }

  // Resolves with the backend AuthResponse.
  // Rejects with err.code: MOCK | NO_CLIENT_ID | LOAD_FAILED | CANCELLED | GOOGLE_ERROR | BACKEND_ERROR | BAD_RESPONSE
  async function signIn() {
    if (MOCK_PENDING) {
      await new Promise((r) => setTimeout(r, 900));
      throw codeError('MOCK');
    }
    if (!GOOGLE_CLIENT_ID || GOOGLE_CLIENT_ID.startsWith('YOUR_')) throw codeError('NO_CLIENT_ID');
    await preload();
    const accessToken = await requestAccessToken();
    return exchangeWithBackend(accessToken);
  }

  // Message to show the user for a rejected signIn() (not used for MOCK / CANCELLED)
  function friendlyError(err) {
    if (err.name === 'AbortError') return 'The server took too long to respond. Please try again.';
    if (err instanceof TypeError) return 'Cannot reach the server. Check that the backend is running on port 8080.';
    switch (err.code) {
      case 'NO_CLIENT_ID':   return 'Google sign-in is not configured yet (missing Google Client ID).';
      case 'LOAD_FAILED':    return 'Could not load Google sign-in. Check your internet connection and try again.';
      case 'GOOGLE_ERROR':   return 'Google sign-in failed. Please try again.';
      case 'BAD_RESPONSE':   return 'Invalid server response. Please try again.';
      case 'BACKEND_ERROR':
        return err.status === 401 || err.status === 403
          ? 'Google could not verify your account. Please try again.'
          : 'Something went wrong on our side. Please try again shortly.';
      default:               return 'Google sign-in failed. Please try again.';
    }
  }

  // Warm up the Google script as soon as the page loads (no-op while mocked)
  preload().catch(() => { /* surfaced again on click */ });

  return { signIn, friendlyError };
})();
