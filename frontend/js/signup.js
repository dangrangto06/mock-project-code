// CodeForge - Sign up page logic
(function () {
  'use strict';

  // ===== Config =====
  const API_BASE = 'http://localhost:8080/api';   // backend: server.port=8080

  const ENDPOINTS = {
    register: `${API_BASE}/auth/register`,   // DONE  - POST { fullName, username, email, password }
    github:   `${API_BASE}/auth/github`      // TODO backend - GET, redirect to GitHub OAuth
  };

  // true  = backend features that don't exist yet are simulated (UI preview only)
  // false = call the real API. Set to false once the backend is ready.
  const MOCK_PENDING_FEATURES = true;

  const REDIRECT_AFTER_SIGNUP = 'index.html#dashboard';
  const TOKEN_KEY = 'codeforge_token';   // same keys as login.js
  const USER_KEY = 'codeforge_user';

  const MIN_PASSWORD_LENGTH = 8;
  const USERNAME_PATTERN = /^[A-Za-z0-9_]{3,20}$/;
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
  const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

  function showToast(message, type) {
    toastEl.textContent = message;
    toastEl.className = 'toast show' + (type ? ' ' + type : '');
    clearTimeout(showToast._t);
    showToast._t = setTimeout(() => toastEl.classList.remove('show'), 2800);
  }

  function setFieldError(inputEl, message) {
    const field = inputEl.closest('.field');
    const errorEl = document.getElementById(inputEl.id + '-error');
    field.classList.toggle('has-error', !!message);
    errorEl.textContent = message || '';
    inputEl.setAttribute('aria-invalid', message ? 'true' : 'false');
  }

  function showFormError(message) {
    formError.textContent = message;
    formError.hidden = !message;
  }

  function setLoading(isLoading) {
    submitBtn.disabled = isLoading;
    submitBtn.classList.toggle('is-loading', isLoading);
    btnLabel.textContent = isLoading ? 'Creating account…' : 'Create account';
  }

  // ===== Password strength =====
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

  function updateStrength() {
    const pw = passwordInput.value;
    const level = scorePassword(pw);
    const labels = ['', 'Weak', 'Fair', 'Good', 'Strong'];
    strengthEl.classList.toggle('is-visible', pw.length > 0);
    strengthEl.dataset.level = String(level);
    strengthLabel.textContent = labels[level];
  }

  // ===== Validation =====
  function validate() {
    let ok = true;
    const fullName = fullNameInput.value.trim();
    const username = usernameInput.value.trim();
    const email = emailInput.value.trim();
    const password = passwordInput.value;

    if (!fullName) { setFieldError(fullNameInput, 'Please enter your full name.'); ok = false; }
    else setFieldError(fullNameInput, '');

    if (!username) { setFieldError(usernameInput, 'Please choose a username.'); ok = false; }
    else if (!USERNAME_PATTERN.test(username)) {
      setFieldError(usernameInput, '3–20 characters: letters, numbers, underscore.');
      ok = false;
    } else setFieldError(usernameInput, '');

    if (!email) { setFieldError(emailInput, 'Please enter your email address.'); ok = false; }
    else if (!EMAIL_PATTERN.test(email)) { setFieldError(emailInput, 'Please enter a valid email address.'); ok = false; }
    else setFieldError(emailInput, '');

    if (!password) { setFieldError(passwordInput, 'Please create a password.'); ok = false; }
    else if (password.length < MIN_PASSWORD_LENGTH) {
      setFieldError(passwordInput, `Password must be at least ${MIN_PASSWORD_LENGTH} characters.`);
      ok = false;
    } else setFieldError(passwordInput, '');

    if (!termsInput.checked) { setFieldError(termsInput, 'Please accept the terms to continue.'); ok = false; }
    else setFieldError(termsInput, '');

    return ok;
  }

  // ===== Auth storage (same format as login.js) =====
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

  // Map a failed request to a message (and highlight the right field when we can tell)
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
        // TODO (backend dev): register() throws RuntimeException for duplicates, which becomes a 500.
        // Return 409 + { "message": "Username is already taken" } so the UI can show a precise error.
        ? 'Could not create your account. The username or email may already be in use.'
        : (err.message || 'Sign up failed. Please try again.');
    }
  }

  // ===== Events =====
  form.addEventListener('submit', async function (e) {
    e.preventDefault();
    showFormError('');
    if (!validate()) {
      (document.querySelector('.has-error input') || fullNameInput).focus();
      return;
    }

    setLoading(true);
    try {
      const data = await register({
        fullName: fullNameInput.value.trim(),
        username: usernameInput.value.trim(),
        email: emailInput.value.trim(),
        password: passwordInput.value
      });
      if (!data || !data.token) throw new Error('Invalid server response.');

      saveSession(data);
      showToast('Account created! Welcome to CodeForge.', 'success');
      setTimeout(() => { window.location.href = REDIRECT_AFTER_SIGNUP; }, 700);
    } catch (err) {
      showFormError(handleRegisterError(err));
      setLoading(false);
    }
  });

  // Clear errors while typing
  [fullNameInput, usernameInput, emailInput, passwordInput].forEach((input) => {
    input.addEventListener('input', () => { setFieldError(input, ''); showFormError(''); });
  });
  termsInput.addEventListener('change', () => setFieldError(termsInput, ''));
  passwordInput.addEventListener('input', updateStrength);

  // Show / hide password
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
      // TODO (backend dev): same OAuth flow as the login page. The backend redirects back with a
      // token; read it from the URL, call saveSession(...) and go to the dashboard.
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
