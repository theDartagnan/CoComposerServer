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
      console.log('Inject csrf token from cookie');
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

async function createAccount(email, firstname, lastname, password, csrfHolder) {
  const answer = await checkAndUpdateCsrf(fetch(`${HOSTNAME}/api/v1/rest/accounts`, {
    method: 'POST',
    headers: createSendHeaders(csrfHolder),
    body: JSON.stringify({
      memberInfo: {
        email, firstname, lastname,
      },
      password,
    }, null, 0),
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

async function getMyCompositions(csrfHolder) {
  const answer = await checkAndUpdateCsrf(fetch(`${HOSTNAME}/api/v1/rest/compositions`, {
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

async function getComposition(compoId, csrfHolder) {
  const answer = await checkAndUpdateCsrf(fetch(`${HOSTNAME}/api/v1/rest/compositions/${compoId}`, {
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

async function doCreateAccountTests() {
  const csrfHolder = null;
  console.log('Check account');
  const res = await getMyself(csrfHolder);
  if (res !== null) {
    console.warn('Test might be biased: user already authentified');
  }

  console.log('Create Account');
  const account = await createAccount('toto@gmail.com', 'Toto', 'Alecol', 'toto1234', csrfHolder);
  console.log('acount created', account);

  console.log('Login');
  const loginResult = await login('toto@gmail.com', 'toto1234', csrfHolder);
  console.log('login achieved', loginResult);

  console.log('Get Myself');
  const myself = await getMyself(csrfHolder);
  console.log('myself retrieved', myself);

  console.log('Logout');
  const logoutRes = await logout(csrfHolder);
  console.log('Logout achieved', logoutRes);

  return true;
}

async function doCreateOnLoginFail() {
  console.log('Check account');
  const res = await getMyself();
  if (res !== null) {
    console.warn('Test might be biased: user already authentified');
  }

  console.log('Login');
  const loginResult = await login('mem1@collamap.com', 'pwd-mem1');
  console.log('login achieved', loginResult);

  console.log('Create Account');
  try {
    await createAccount('badaccount@gmail.com', 'Toto', 'Alecol', 'toto1234');
    throw new Error('Creation of an account should fail');
  } catch (error) {
    console.log('The creation account request failed successfully');
    console.log(error);
  }

  console.log('Logout');
  const logoutRes = await logout();
  console.log('Logout achieved', logoutRes);

  return true;
}

async function doGetCompositions() {
  console.log('Check account');
  const res = await getMyself();
  if (res !== null) {
    console.warn('Test might be biased: user already authentified');
  }

  console.log('Login');
  const loginResult = await login('mem2@collamap.com', 'pwd-mem2');
  console.log('login achieved', loginResult);

  console.log('Get compositions');
  const compositions = await getMyCompositions();
  console.log('My compositions', compositions);

  if (!compositions.ownedCompositions.length) {
    throw new Error('The use should have at least one owned compositions');
  }
  const compoId = compositions.ownedCompositions[0].id;

  const composition = await getComposition(compoId);
  console.log('The composition', composition);

  console.log('Logout');
  const logoutRes = await logout();
  console.log('Logout achieved', logoutRes);

  return true;
}

doGetCompositions().catch((error) => {
  console.warn('Error happend');
  console.error(error);
});
