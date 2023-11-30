/* eslint-disable no-console, no-param-reassign, no-unused-vars */

const HOSTNAME = 'http://localhost:8080';

function createCsrfHolder() {
  return {
    name: null,
    token: null,
  };
}

function getCrsfTokenFromCookie(cookieName = 'XSRF-TOKEN') {
  return document.cookie.split(';').map((c) => c.trim()).filter((c) => c.startsWith(cookieName)).map((c) => c.split('=')[1])[0];
}

function createSendHeaders(csrfHolder) {
  const base = { 'Content-Type': 'application/json' };
  if (csrfHolder && csrfHolder.name) {
    console.log('Inject csrf token from holder');
    base[csrfHolder.name] = csrfHolder.token;
  } else {
    const csrfCookie = getCrsfTokenFromCookie();
    if (csrfCookie) {
      // console.log('Inject csrf token from cookie');
      base['X-XSRF-TOKEN'] = csrfCookie;
    }
  }
  return new Headers(base);
}

async function checkAndUpdateCsrf(promise, csrfHolder) {
  const answer = await promise;
  if (csrfHolder && answer.headers) {
    if (answer.headers.has('X-TOKEN-CSRF')) {
      console.log('New CSRF Token received!');
      csrfHolder.name = 'X-TOKEN-CSRF';
      csrfHolder.token = answer.headers.get(csrfHolder.name);
    } else if (answer.headers.has('X-TOKEN-XSRF')) {
      console.log('New CSRF Token received!');
      csrfHolder.name = 'X-TOKEN-XSRF';
      csrfHolder.token = answer.headers.get(csrfHolder.name);
    }
  }
  return answer;
}

function validateAnswer(answer) {
  if (!answer.ok) {
    throw new Error(`Error of status ${answer.status}`, answer);
  }
}

async function login(username, password, csrfHolder) {
  const answer = await checkAndUpdateCsrf(fetch(`${HOSTNAME}/api/login`, {
    method: 'POST',
    headers: createSendHeaders(csrfHolder),
    body: JSON.stringify({ username, password }, null, 0),
    credentials: 'include',
  }));
  validateAnswer(answer);
  if (answer.status === 200) {
    return answer.json();
  }
  return null;
}

async function logout(csrfHolder) {
  const answer = await checkAndUpdateCsrf(fetch(`${HOSTNAME}/api/logout`, {
    method: 'POST',
    headers: createSendHeaders(csrfHolder),
    credentials: 'include',
  }));
  validateAnswer(answer);
  if (answer.status === 200) {
    return answer.json();
  }
  return null;
}

async function getMyself(csrfHolder) {
  const answer = await checkAndUpdateCsrf(fetch(`${HOSTNAME}/api/v1/rest/accounts/myself`, {
    method: 'GET',
    headers: createSendHeaders(csrfHolder),
    credentials: 'include',
  }));
  validateAnswer(answer);
  if (answer.status === 200) {
    return answer.json();
  }
  return null;
}