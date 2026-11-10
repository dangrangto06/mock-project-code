// CodeForge - Login page logic
(function () {
  'use strict';

  // ===== Config =====
  const API_BASE = 'http://localhost:8080/api';   // backend: server.port=8080

  const ENDPOINTS = {
    login:          `${API_BASE}/auth/login`,            // DONE  - POST { usernameOrEmail, password }
    forgotPassword: `${API_BASE}/auth/forgot-password`,  // TODO backend - POST { email }
    github:         `${API_BASE}/auth/github`            // TODO backend - GET, redirect to GitHub OAuth
  };

  // true  = backend features that don't exist yet are simulated (UI preview only)
  // false = call the real API. Set to false once the backend is ready.
  const MOCK_PENDING_FEATURES = true;

  const REDIRECT_AFTER_LOGIN = 'index.html#dashboard';
  const TOKEN_KEY = 'codeforge_token';
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
    btnLabel.textContent = isLoading ? 'Signing in…' : 'Sign in to your workspace';
  }

  // ===== Validation =====
  function validate() {
    let ok = true;
    const identity = emailInput.value.trim();
    const password = passwordInput.value;

    if (!identity) {
      setFieldError(emailInput, 'Please enter your email or username.');
      ok = false;
    } else if (identity.includes('@') && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(identity)) {
      setFieldError(emailInput, 'Please enter a valid email address.');
      ok = false;
    } else {
      setFieldError(emailInput, '');
    }

    if (!password) {
      setFieldError(passwordInput, 'Please enter your password.');
      ok = false;
    } else {
      setFieldError(passwordInput, '');
    }
    return ok;
  }

  // ===== Auth storage =====
  function saveSession(data, remember) {
    // "Keep me signed in" -> localStorage, otherwise cleared when the tab closes
    const store = remember ? localStorage : sessionStorage;
    const other = remember ? sessionStorage : localStorage;
    try {
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
  async function login(usernameOrEmail, password) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 10000);

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
  form.addEventListener('submit', async function (e) {
    e.preventDefault();
    showFormError('');
    if (!validate()) {
      (document.querySelector('.has-error input') || emailInput).focus();
      return;
    }

    setLoading(true);
    try {
      const data = await login(emailInput.value.trim(), passwordInput.value);
      if (!data || !data.token) throw new Error('Invalid server response.');

      saveSession(data, rememberInput.checked);
      showToast('Welcome back' + (data.fullName ? ', ' + data.fullName : '') + '!', 'success');
      setTimeout(() => { window.location.href = REDIRECT_AFTER_LOGIN; }, 600);
    } catch (err) {
      showFormError(friendlyError(err));
      setLoading(false);
      passwordInput.select();
    }
  });

  // Clear errors while typing
  emailInput.addEventListener('input', () => { setFieldError(emailInput, ''); showFormError(''); });
  passwordInput.addEventListener('input', () => { setFieldError(passwordInput, ''); showFormError(''); });

  // Show / hide password
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

  async function startGithubLogin() {
    githubBtn.disabled = true;
    githubBtn.textContent = 'Connecting to GitHub…';
    try {
      if (MOCK_PENDING_FEATURES) {
        await sleep(900);
        showToast('GitHub sign-in is not connected to the backend yet.');
        return;
      }
      // TODO (backend dev): the backend usually redirects to GitHub OAuth, then redirects
      // back to this page with a token (e.g. login.html?token=...). Add code that reads
      // the token from the URL, calls saveSession(...) and goes to the dashboard.
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
    document.body.style.overflow = 'hidden';
    setTimeout(() => forgotEmail.focus(), 50);
  }

  function closeForgotModal() {
    modal.hidden = true;
    document.body.style.overflow = '';
    if (lastFocused) lastFocused.focus();
  }

  async function requestPasswordReset(email) {
    if (MOCK_PENDING_FEATURES) {
      await sleep(900);
      return;
    }
    // TODO (backend dev): POST { email } -> 200 OK.
    // Should return 200 whether or not the email exists (avoids leaking accounts).
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

  document.getElementById('forgot-link').addEventListener('click', (e) => {
    e.preventDefault();
    openForgotModal();
  });

  modal.addEventListener('click', (e) => {
    if (e.target.closest('[data-close]')) closeForgotModal();
  });
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && !modal.hidden) closeForgotModal();
  });

  forgotEmail.addEventListener('input', () => {
    setFieldError(forgotEmail, '');
    forgotFormError.hidden = true;
  });

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
  } catch (_) { /* storage blocked */ }
})();
