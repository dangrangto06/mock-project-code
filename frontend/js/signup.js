/**
 * @file signup.js
 * @description CodeForge Registration & Account Creation Controller.
 *
 * <p>Architectural Role:</p>
 * Controls the dedicated sign-up view (`signup.html`), providing live client-side validation,
 * real-time password entropy/strength metering, terms acceptance checks, automated conflict error mapping
 * (e.g. detecting duplicate usernames or emails), and dispatching registration payloads to
 * the Spring Boot backend (`POST /api/auth/register`).
 */

(function () {
  'use strict';

  // ===== Config =====

  /** Base URL for backend REST API endpoints */
  const API_BASE = 'http://localhost:8080/api';   // backend: server.port=8080

  /** Endpoints utilized by the registration flow */
  const ENDPOINTS = {
    register: `${API_BASE}/auth/register`,   // Live endpoint - POST { fullName, username, email, password }
    github:   `${API_BASE}/auth/github`      // OAuth gateway - GET, redirect to GitHub OAuth
  };

  /**
   * Flag determining whether endpoints not yet fully implemented on the backend
   * should be gracefully simulated for UI demonstration.
   */
  const MOCK_PENDING_FEATURES = true;

  /** Target route navigated to upon successful account registration */
  const REDIRECT_AFTER_SIGNUP = 'index.html#dashboard';

  /** Storage keys matching login.js for cross-page session continuity */
  const TOKEN_KEY = 'codeforge_token';
  const USER_KEY = 'codeforge_user';

  /** Password minimum character threshold */
  const MIN_PASSWORD_LENGTH = 8;

  /** Regular expression validating allowed username characters and length (3-20 alphanumeric or underscore) */
  const USERNAME_PATTERN = /^[A-Za-z0-9_]{3,20}$/;

  /** Standard RFC 5322-compliant email regex pattern */
  const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

  // ===== Elements =====
  const form = document.getElementById('signup-form');
  const fullNameInput = document.getElementById('fullName');
  const usernameInput = document.getElementById('username');
  const emailInput = document.getElementById('email');
  const passwordInput = document.getElementById('password');
  const termsInput = document.getElementById('terms');
  const submitBtn = document.getElementById('btn-submit');
  const btnLabel = submitBtn.querySelector('.btn-label');
  const toggleBtn = document.getElementById('toggle-password');
  const formError = document.getElementById('form-error');
  const strengthEl = document.getElementById('strength');
  const strengthLabel = document.getElementById('strength-label');
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
   * @param {string} [type] - Optional CSS modifier (e.g. 'success', 'warn').
   */
  function showToast(message, type) {
    toastEl.textContent = message;
    toastEl.className = 'toast show' + (type ? ' ' + type : '');
    clearTimeout(showToast._t);
    showToast._t = setTimeout(() => toastEl.classList.remove('show'), 2800);
  }

  /**
   * Toggles error states and accessibility attributes on form field containers.
   *
   * @param {HTMLInputElement} inputEl - Target input DOM element.
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
   * Renders a top-level banner error message above the registration form.
   *
   * @param {string} message - Error description text, or empty string to hide.
   */
  function showFormError(message) {
    formError.textContent = message;
    formError.hidden = !message;
  }

  /**
   * Toggles the loading spinner and disabled status of the submit button.
   *
   * @param {boolean} isLoading - Whether an asynchronous registration request is in progress.
   */
  function setLoading(isLoading) {
    submitBtn.disabled = isLoading;
    submitBtn.classList.toggle('is-loading', isLoading);
    btnLabel.textContent = isLoading ? 'Creating account…' : 'Create account';
  }

  // ===== Password strength =====

  /**
   * Computes password complexity score on a scale from 1 (Weak) to 4 (Strong).
   *
   * <p>Scoring Rules:</p>
   * <ul>
   *   <li>+1: Length &ge; 8 characters.</li>
   *   <li>+1: Mixed case (both lowercase and uppercase letters).</li>
   *   <li>+1: Contains at least one numeric digit.</li>
   *   <li>+1: Contains special character symbols or length &ge; 12.</li>
   * </ul>
   *
   * @param {string} pw - Plaintext password to evaluate.
   * @returns {number} Score from 0 to 4.
   */
  function scorePassword(pw) {
    if (!pw) return 0;
    let score = 0;
    if (pw.length >= MIN_PASSWORD_LENGTH) score++;
    if (/[a-z]/.test(pw) && /[A-Z]/.test(pw)) score++;
    if (/\d/.test(pw)) score++;
    if (/[^A-Za-z0-9]/.test(pw) || pw.length >= 12) score++;
    // Too short can never rate above "Weak"
    if (pw.length < MIN_PASSWORD_LENGTH) return 1;
    return Math.max(score, 1);
  }

  /**
   * Updates visual password strength meter UI bar and textual label.
   */
  function updateStrength() {
    const pw = passwordInput.value;
    const level = scorePassword(pw);
    const labels = ['', 'Weak', 'Fair', 'Good', 'Strong'];
    strengthEl.classList.toggle('is-visible', pw.length > 0);
    strengthEl.dataset.level = String(level);
    strengthLabel.textContent = labels[level];
  }

  // ===== Validation =====

  /**
   * Validates all registration fields prior to submission.
   *
   * @returns {boolean} True if all validation criteria are met; false otherwise.
   */
  function validate() {
    let ok = true;
    const fullName = fullNameInput.value.trim();
    const username = usernameInput.value.trim();
    const email = emailInput.value.trim();
    const password = passwordInput.value;

    // Full name check
    if (!fullName) { setFieldError(fullNameInput, 'Please enter your full name.'); ok = false; }
    else setFieldError(fullNameInput, '');

    // Username format check
    if (!username) { setFieldError(usernameInput, 'Please choose a username.'); ok = false; }
    else if (!USERNAME_PATTERN.test(username)) {
      setFieldError(usernameInput, '3–20 characters: letters, numbers, underscore.');
      ok = false;
    } else setFieldError(usernameInput, '');

    // Email format check
    if (!email) { setFieldError(emailInput, 'Please enter your email address.'); ok = false; }
    else if (!EMAIL_PATTERN.test(email)) { setFieldError(emailInput, 'Please enter a valid email address.'); ok = false; }
    else setFieldError(emailInput, '');

    // Password length check
    if (!password) { setFieldError(passwordInput, 'Please create a password.'); ok = false; }
    else if (password.length < MIN_PASSWORD_LENGTH) {
      setFieldError(passwordInput, `Password must be at least ${MIN_PASSWORD_LENGTH} characters.`);
      ok = false;
    } else setFieldError(passwordInput, '');

    // Terms of service checkbox check
    if (!termsInput.checked) { setFieldError(termsInput, 'Please accept the terms to continue.'); ok = false; }
    else setFieldError(termsInput, '');

    return ok;
  }

  // ===== Auth storage (same format as login.js) =====

  /**
   * Caches issued JWT access token and user identity in persistent {@code localStorage}.
   *
   * @param {Object} data - Authentication response payload from backend.
   */
  function saveSession(data) {
    try {
      sessionStorage.removeItem(TOKEN_KEY);
      sessionStorage.removeItem(USER_KEY);
      localStorage.setItem(TOKEN_KEY, data.token);
      localStorage.setItem(USER_KEY, JSON.stringify({
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

  // ===== Register API =====

  /**
   * Dispatches user registration payload to the Spring Boot REST API.
   *
   * @async
   * @param {Object} payload - Registration details.
   * @param {string} payload.fullName - User's full name.
   * @param {string} payload.username - User's unique handle.
   * @param {string} payload.email - User's unique email.
   * @param {string} payload.password - Plaintext password.
   * @returns {Promise<Object>} Backend authentication response object.
   * @throws {Error} Throws on network failure or HTTP error status.
   */
  async function register(payload) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 10000);

    try {
      const res = await fetch(ENDPOINTS.register, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify(payload),
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
   * Maps backend error responses to specific form inputs and returns user-friendly messages.
   *
   * <p>Detects unique constraint collisions (username or email already taken) and highlights
   * the appropriate field with focused styling.</p>
   *
   * @param {Error} err - Captured error object.
   * @returns {string} Error message string to render in banner.
   */
  function handleRegisterError(err) {
    if (err.name === 'AbortError') return 'The server took too long to respond. Please try again.';
    if (err instanceof TypeError) return 'Cannot reach the server. Check that the backend is running on port 8080.';

    const msg = (err.message || '').toLowerCase();
    if (msg.includes('username')) {
      setFieldError(usernameInput, 'This username is already taken.');
      usernameInput.focus();
      return '';
    }
    if (msg.includes('email')) {
      setFieldError(emailInput, 'This email is already registered.');
      emailInput.focus();
      return '';
    }

    switch (err.status) {
      case 400: return 'Please check the information you entered.';
      case 409: return 'This username or email is already in use.';
      default:  return err.status >= 500
        ? 'Could not create your account. The username or email may already be in use.'
        : (err.message || 'Sign up failed. Please try again.');
    }
  }

  // ===== Events =====

  // Attach submit listener to the registration form
  form.addEventListener('submit', async function (e) {
    e.preventDefault();
    showFormError('');

    // Pre-flight client validation
    if (!validate()) {
      (document.querySelector('.has-error input') || fullNameInput).focus();
      return;
    }

    setLoading(true);
    try {
      // Dispatch registration payload to backend
      const data = await register({
        fullName: fullNameInput.value.trim(),
        username: usernameInput.value.trim(),
        email: emailInput.value.trim(),
        password: passwordInput.value
      });
      if (!data || !data.token) throw new Error('Invalid server response.');

      // Save credentials and redirect to dashboard
      saveSession(data);
      showToast('Account created! Welcome to CodeForge.', 'success');
      setTimeout(() => { window.location.href = REDIRECT_AFTER_SIGNUP; }, 700);
    } catch (err) {
      showFormError(handleRegisterError(err));
      setLoading(false);
    }
  });

  // Clear validation warnings dynamically on input
  [fullNameInput, usernameInput, emailInput, passwordInput].forEach((input) => {
    input.addEventListener('input', () => { setFieldError(input, ''); showFormError(''); });
  });
  termsInput.addEventListener('change', () => setFieldError(termsInput, ''));
  passwordInput.addEventListener('input', updateStrength);

  // Toggle password visibility
  toggleBtn.addEventListener('click', function () {
    const show = passwordInput.type === 'password';
    passwordInput.type = show ? 'text' : 'password';
    toggleBtn.setAttribute('aria-pressed', String(show));
    toggleBtn.setAttribute('aria-label', show ? 'Hide password' : 'Show password');
    passwordInput.focus();
  });

  // Terms / privacy links are placeholders for now
  document.querySelectorAll('.inline-link').forEach((link) => {
    link.addEventListener('click', (e) => e.preventDefault());
  });

  // ===== GitHub sign-up =====
  const githubBtn = document.getElementById('btn-github');
  const githubBtnHTML = githubBtn.innerHTML;

  githubBtn.addEventListener('click', async function () {
    githubBtn.disabled = true;
    githubBtn.textContent = 'Connecting to GitHub…';
    try {
      if (MOCK_PENDING_FEATURES) {
        await sleep(900);
        showToast('GitHub sign-up is not connected to the backend yet.');
        return;
      }
      window.location.href = ENDPOINTS.github;
    } finally {
      githubBtn.disabled = false;
      githubBtn.innerHTML = githubBtnHTML;
    }
  });

  // ===== Already signed in -> skip the sign-up page =====
  try {
    if (localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY)) {
      window.location.replace(REDIRECT_AFTER_SIGNUP);
    }
  } catch (_) { /* storage blocked */ }
})();
