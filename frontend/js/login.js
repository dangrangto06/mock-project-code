/**
 * @file login.js
 * @description CodeForge Authentication & Login Page Controller.
 *
 * <p>Architectural Role:</p>
 * Manages the dedicated login view (`login.html`), handling client-side form validation,
 * credential dispatch to the Spring Boot backend (`POST /api/auth/login`), persistent vs.
 * session-based JWT storage handling, password visibility toggling, GitHub OAuth simulation,
 * and the interactive Forgot Password modal workflow.
 */

(function () {
  'use strict';

  // ===== Config =====

  /** Base URL for backend REST API endpoints */
  const API_BASE = 'http://localhost:8080/api';   // backend: server.port=8080

  /** Authentication API route definitions */
  const ENDPOINTS = {
    login:          `${API_BASE}/auth/login`,            // Live endpoint - POST { usernameOrEmail, password }
    forgotPassword: `${API_BASE}/auth/forgot-password`,  // Backend route - POST { email }
    github:         `${API_BASE}/auth/github`            // OAuth gateway - GET, redirect to GitHub OAuth
  };

  /**
   * Flag determining whether endpoints not yet fully implemented on the backend
   * should be gracefully simulated for UI demonstration.
   */
  const MOCK_PENDING_FEATURES = true;

  /** Target route navigated to upon successful credential verification */
  const REDIRECT_AFTER_LOGIN = 'index.html#dashboard';

  /** Storage key for persisting the JWT token */
  const TOKEN_KEY = 'codeforge_token';

  /** Storage key for persisting the serialized user profile */
  const USER_KEY = 'codeforge_user';

  // ===== Elements =====
  const form = document.getElementById('login-form');
  const emailInput = document.getElementById('email');
  const passwordInput = document.getElementById('password');
  const rememberInput = document.getElementById('remember');
  const submitBtn = document.getElementById('btn-submit');
  const btnLabel = submitBtn.querySelector('.btn-label');
  const toggleBtn = document.getElementById('toggle-password');
  const formError = document.getElementById('form-error');
  const toastEl = document.getElementById('toast');

  // ===== Helpers =====

  /**
   * Utility helper returning a Promise that resolves after a specified delay.
   *
   * @param {number} ms - Milliseconds to sleep.
   * @returns {Promise<void>}
   */
  const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

  /**
   * Displays an animated toast notification on screen.
   *
   * @param {string} message - Notification text to display.
   * @param {string} [type] - Optional CSS style modifier (e.g. 'success', 'warn').
   */
  function showToast(message, type) {
    toastEl.textContent = message;
    toastEl.className = 'toast show' + (type ? ' ' + type : '');
    clearTimeout(showToast._t);
    showToast._t = setTimeout(() => toastEl.classList.remove('show'), 2800);
  }

  /**
   * Sets or clears visual error indicators and aria-invalid attributes on form fields.
   *
   * @param {HTMLInputElement} inputEl - The target input DOM element.
   * @param {string} message - Error description text, or empty string to clear.
   */
  function setFieldError(inputEl, message) {
    const field = inputEl.closest('.field');
    const errorEl = document.getElementById(inputEl.id + '-error');
    field.classList.toggle('has-error', !!message);
    errorEl.textContent = message || '';
    inputEl.setAttribute('aria-invalid', message ? 'true' : 'false');
  }

  /**
   * Renders a top-level banner error message above the login form.
   *
   * @param {string} message - Error description text, or empty string to hide.
   */
  function showFormError(message) {
    formError.textContent = message;
    formError.hidden = !message;
  }

  /**
   * Toggles the loading state, spinner, and disabled status of the submit button.
   *
   * @param {boolean} isLoading - Whether an asynchronous request is in flight.
   */
  function setLoading(isLoading) {
    submitBtn.disabled = isLoading;
    submitBtn.classList.toggle('is-loading', isLoading);
    btnLabel.textContent = isLoading ? 'Signing in…' : 'Sign in to your workspace';
  }

  // ===== Validation =====

  /**
   * Validates user inputs prior to network transmission.
   *
   * <p>Checks that the username/email is non-empty, checks email format if an '@' symbol
   * is present, and verifies that the password field is populated.</p>
   *
   * @returns {boolean} True if all inputs pass validation; false otherwise.
   */
  function validate() {
    let ok = true;
    const identity = emailInput.value.trim();
    const password = passwordInput.value;

    // Validate identity (username or email)
    if (!identity) {
      setFieldError(emailInput, 'Please enter your email or username.');
      ok = false;
    } else if (identity.includes('@') && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(identity)) {
      setFieldError(emailInput, 'Please enter a valid email address.');
      ok = false;
    } else {
      setFieldError(emailInput, '');
    }

    // Validate password field
    if (!password) {
      setFieldError(passwordInput, 'Please enter your password.');
      ok = false;
    } else {
      setFieldError(passwordInput, '');
    }
    return ok;
  }

  // ===== Auth storage =====

  /**
   * Persists authenticated user profile and JWT token into Web Storage.
   *
   * <p>Storage Strategy:</p>
   * <ul>
   *   <li>If {@code remember} is true ("Keep me signed in"): Saves to {@code localStorage}
   *       (persisting across browser restarts).</li>
   *   <li>If {@code remember} is false: Saves to {@code sessionStorage}
   *       (cleared automatically when the browser tab closes).</li>
   * </ul>
   *
   * @param {Object} data - Authentication response payload from backend.
   * @param {string} data.token - JWT Bearer token.
   * @param {number} data.id - User primary key ID.
   * @param {string} data.username - User handle.
   * @param {string} data.email - User email address.
   * @param {string} data.fullName - User display name.
   * @param {string} data.role - User authorization role.
   * @param {boolean} remember - Whether the "Remember me" option was checked.
   */
  function saveSession(data, remember) {
    const store = remember ? localStorage : sessionStorage;
    const other = remember ? sessionStorage : localStorage;
    try {
      // Clear opposite storage container to prevent stale token collisions
      other.removeItem(TOKEN_KEY);
      other.removeItem(USER_KEY);
      store.setItem(TOKEN_KEY, data.token);
      store.setItem(USER_KEY, JSON.stringify({
        id: data.id,
        username: data.username,
        email: data.email,
        fullName: data.fullName,
        role: data.role
      }));
    } catch (e) {
      console.warn('Cannot access web storage', e);
    }
  }

  // ===== Login API =====

  /**
   * Sends an HTTP POST request to authenticate credentials against the backend.
   *
   * @async
   * @param {string} usernameOrEmail - Username handle or registered email.
   * @param {string} password - Plaintext password candidate.
   * @returns {Promise<Object>} Backend authentication response object.
   * @throws {Error} Throws with HTTP status code and message if verification fails.
   */
  async function login(usernameOrEmail, password) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 10000); // 10s request timeout

    try {
      const res = await fetch(ENDPOINTS.login, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify({ usernameOrEmail, password }),
        signal: controller.signal
      });

      let body = null;
      try { body = await res.json(); } catch (_) { /* empty / non-JSON body */ }

      if (!res.ok) {
        const err = new Error((body && body.message) || '');
        err.status = res.status;
        throw err;
      }
      return body;
    } finally {
      clearTimeout(timeout);
    }
  }

  /**
   * Translates network and HTTP error responses into friendly, human-readable user guidance.
   *
   * @param {Error} err - Captured error object.
   * @returns {string} User-facing error message string.
   */
  function friendlyError(err) {
    if (err.name === 'AbortError') return 'The server took too long to respond. Please try again.';
    if (err instanceof TypeError) return 'Cannot reach the server. Check that the backend is running on port 8080.';
    switch (err.status) {
      case 400: return 'Please check your email/username and password.';
      case 401:
      case 403: return 'Incorrect email/username or password.';
      case 404: return 'Login service not found. Please contact support.';
      default:  return err.status >= 500
        ? 'Something went wrong on our side. Please try again shortly.'
        : (err.message || 'Sign in failed. Please try again.');
    }
  }

  // ===== Login form events =====

  // Attach submit handler to the login form
  form.addEventListener('submit', async function (e) {
    e.preventDefault();
    showFormError('');

    // Pre-flight validation check
    if (!validate()) {
      (document.querySelector('.has-error input') || emailInput).focus();
      return;
    }

    setLoading(true);
    try {
      // Dispatch authentication request to the backend
      const data = await login(emailInput.value.trim(), passwordInput.value);
      if (!data || !data.token) throw new Error('Invalid server response.');

      // Persist session tokens and navigate to dashboard
      saveSession(data, rememberInput.checked);
      showToast('Welcome back' + (data.fullName ? ', ' + data.fullName : '') + '!', 'success');
      setTimeout(() => { window.location.href = REDIRECT_AFTER_LOGIN; }, 600);
    } catch (err) {
      showFormError(friendlyError(err));
      setLoading(false);
      passwordInput.select();
    }
  });

  // Clear validation warnings dynamically as the user types
  emailInput.addEventListener('input', () => { setFieldError(emailInput, ''); showFormError(''); });
  passwordInput.addEventListener('input', () => { setFieldError(passwordInput, ''); showFormError(''); });

  // Toggle plaintext vs hidden password input visibility
  toggleBtn.addEventListener('click', function () {
    const show = passwordInput.type === 'password';
    passwordInput.type = show ? 'text' : 'password';
    toggleBtn.setAttribute('aria-pressed', String(show));
    toggleBtn.setAttribute('aria-label', show ? 'Hide password' : 'Show password');
    passwordInput.focus();
  });

  // ===== GitHub sign-in =====
  const githubBtn = document.getElementById('btn-github');
  const githubBtnHTML = githubBtn.innerHTML;

  /**
   * Initiates the GitHub OAuth sign-in flow.
   *
   * @async
   */
  async function startGithubLogin() {
    githubBtn.disabled = true;
    githubBtn.textContent = 'Connecting to GitHub…';
    try {
      if (MOCK_PENDING_FEATURES) {
        await sleep(900);
        showToast('GitHub sign-in is not connected to the backend yet.');
        return;
      }
      // Redirect to backend OAuth gateway when connected
      window.location.href = ENDPOINTS.github;
    } finally {
      githubBtn.disabled = false;
      githubBtn.innerHTML = githubBtnHTML;
    }
  }
  githubBtn.addEventListener('click', startGithubLogin);

  // ===== Forgot password modal =====
  const modal = document.getElementById('forgot-modal');
  const forgotForm = document.getElementById('forgot-form');
  const forgotEmail = document.getElementById('forgot-email');
  const forgotBtn = document.getElementById('btn-forgot-submit');
  const forgotBtnLabel = forgotBtn.querySelector('.btn-label');
  const forgotFormError = document.getElementById('forgot-form-error');
  const viewForm = document.getElementById('forgot-view-form');
  const viewSuccess = document.getElementById('forgot-view-success');
  let lastFocused = null;

  /**
   * Opens the password reset modal and pre-fills email if already typed.
   */
  function openForgotModal() {
    lastFocused = document.activeElement;
    // Prefill with the email already typed in the login form
    const typed = emailInput.value.trim();
    forgotEmail.value = typed.includes('@') ? typed : '';
    setFieldError(forgotEmail, '');
    forgotFormError.hidden = true;
    viewForm.hidden = false;
    viewSuccess.hidden = true;
    modal.hidden = false;
    document.body.style.overflow = 'hidden'; // Prevent background scrolling
    setTimeout(() => forgotEmail.focus(), 50);
  }

  /**
   * Closes the password reset modal and restores user focus.
   */
  function closeForgotModal() {
    modal.hidden = true;
    document.body.style.overflow = '';
    if (lastFocused) lastFocused.focus();
  }

  /**
   * Dispatches password reset request to backend or simulates dispatch.
   *
   * @async
   * @param {string} email - Destination email address.
   * @returns {Promise<void>}
   */
  async function requestPasswordReset(email) {
    if (MOCK_PENDING_FEATURES) {
      await sleep(900);
      return;
    }
    const res = await fetch(ENDPOINTS.forgotPassword, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email })
    });
    if (!res.ok) {
      const err = new Error('Request failed');
      err.status = res.status;
      throw err;
    }
  }

  // Attach modal trigger listeners
  document.getElementById('forgot-link').addEventListener('click', (e) => {
    e.preventDefault();
    openForgotModal();
  });

  // Close modal when clicking on background backdrop or close button
  modal.addEventListener('click', (e) => {
    if (e.target.closest('[data-close]')) closeForgotModal();
  });

  // Close modal on Escape key press
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && !modal.hidden) closeForgotModal();
  });

  // Clear modal errors on typing
  forgotEmail.addEventListener('input', () => {
    setFieldError(forgotEmail, '');
    forgotFormError.hidden = true;
  });

  // Handle forgot password form submission
  forgotForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = forgotEmail.value.trim();

    if (!email) { setFieldError(forgotEmail, 'Please enter your email address.'); return; }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      setFieldError(forgotEmail, 'Please enter a valid email address.');
      return;
    }

    forgotBtn.disabled = true;
    forgotBtn.classList.add('is-loading');
    forgotBtnLabel.textContent = 'Sending…';
    try {
      await requestPasswordReset(email);
      document.getElementById('forgot-sent-to').textContent = email;
      viewForm.hidden = true;
      viewSuccess.hidden = false;
    } catch (err) {
      forgotFormError.textContent = err instanceof TypeError
        ? 'Cannot reach the server. Please try again.'
        : 'Could not send the reset link. Please try again.';
      forgotFormError.hidden = false;
    } finally {
      forgotBtn.disabled = false;
      forgotBtn.classList.remove('is-loading');
      forgotBtnLabel.textContent = 'Send reset link';
    }
  });

  // ===== Already signed in -> skip the login page =====
  try {
    if (localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY)) {
      window.location.replace(REDIRECT_AFTER_LOGIN);
    }
  } catch (_) { /* storage blocked by security policy */ }
})();
