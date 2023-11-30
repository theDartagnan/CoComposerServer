/* eslint-disable no-console, no-param-reassign, no-unused-vars */

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

async function deleteAccount(userId, csrfHolder) {
  const answer = await checkAndUpdateCsrf(fetch(`${HOSTNAME}/api/v1/rest/accounts/${userId}`, {
    method: 'DELETE',
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

async function doCreateDeleteAccountTests() {
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

  console.log('Delete my account');
  const resDel = await deleteAccount(myself.id, csrfHolder);
  console.log('myself retrieved', resDel);

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
  console.log('My compositions retrieved');

  if (!compositions.ownedCompositions.length) {
    throw new Error('The use should have at least one owned compositions');
  }
  const compoId = compositions.ownedCompositions[0].id;

  const composition = await getComposition(compoId);
  console.log('Retrieved composition');

  console.log('Logout');
  const logoutRes = await logout();
  console.log('Logout achieved', logoutRes);

  return true;
}

async function doTests() {
  console.log("START ACCOUNT TESTS");
  try {
    console.log("CREATE / DELETE ACCOUNT TEST");
    await doCreateDeleteAccountTests();

    console.log("CREATE FAIL WHEN ALREADY LOGGED IN");
    await doCreateOnLoginFail();

    console.log("LOG THEN ACCESS AUTHENCIATED SERVICE")
    await doGetCompositions();

    console.log("ALL ACCOUNT TESTS SUCCEEDED.");
  } catch (error) {
    console.warn('Error happend');
    console.error(error);
    console.log("ACCOUNT TESTS FAILED.");
  }
}

doTests();
