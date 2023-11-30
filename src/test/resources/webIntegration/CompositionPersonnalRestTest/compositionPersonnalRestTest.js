/* eslint-disable no-console, no-param-reassign, no-unused-vars */

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

async function createComposition(title, csrfHolder) {
  const answer = await checkAndUpdateCsrf(fetch(`${HOSTNAME}/api/v1/rest/compositions`, {
    method: 'POST',
    headers: createSendHeaders(csrfHolder),
    body: JSON.stringify({
      title,
    }, null, 0),
    credentials: 'include',
  }));
  validateAnswer(answer);
  if (answer.status === 200) {
    return answer.json();
  }
}

async function deleteComposition(compositionId, csrfHolder) {
  const answer = await checkAndUpdateCsrf(fetch(`${HOSTNAME}/api/v1/rest/compositions/${compositionId}`, {
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

async function changeCompositionTitle(compositionId, title, csrfHolder) {
  const answer = await checkAndUpdateCsrf(fetch(`${HOSTNAME}/api/v1/rest/compositions/${compositionId}`, {
    method: 'PATCH',
    headers: createSendHeaders(csrfHolder),
    body: JSON.stringify({
      title,
    }, null, 0),
    credentials: 'include',
  }));
  validateAnswer(answer);
  if (answer.status === 200) {
    return answer.json();
  }
}

async function changeCompositionCollaborative(compositionId, collaborative, csrfHolder) {
  const answer = await checkAndUpdateCsrf(fetch(`${HOSTNAME}/api/v1/rest/compositions/${compositionId}`, {
    method: 'PATCH',
    headers: createSendHeaders(csrfHolder),
    body: JSON.stringify({
      collaborative,
    }, null, 0),
    credentials: 'include',
  }));
  validateAnswer(answer);
  if (answer.status === 200) {
    return answer.json();
  }
}

async function createElement(compoId, elemType, x, y, style = null, extraProps = null, csrfHolder = null) {
  const element = extraProps
    ? { ...extraProps, elementType: elemType, x, y, style }
    : { elementType: elemType, x, y, style };
  const answer = await checkAndUpdateCsrf(fetch(`${HOSTNAME}/api/v1/rest/compositions/${compoId}/elements`, {
    method: 'POST',
    headers: createSendHeaders(csrfHolder),
    body: JSON.stringify(element, null, 0),
    credentials: 'include',
  }));
  validateAnswer(answer);
  if (answer.status === 200) {
    return answer.json();
  }
  return null;
}

async function updateElement(compoId, element, csrfHolder) {
  if (!element.id) {
    throw new Error('Element must have an id');
  }
  const answer = await checkAndUpdateCsrf(fetch(`${HOSTNAME}/api/v1/rest/compositions/${compoId}/elements/${element.id}`, {
    method: 'PUT',
    headers: createSendHeaders(csrfHolder),
    body: JSON.stringify(element, null, 0),
    credentials: 'include',
  }));
  validateAnswer(answer);
  if (answer.status === 200) {
    return answer.json();
  }
  return null;
}

async function updateElementPosition(compoId, elementId, x, y, csrfHolder) {
  const answer = await checkAndUpdateCsrf(fetch(`${HOSTNAME}/api/v1/rest/compositions/${compoId}/elements/${elementId}/position`, {
    method: 'PUT',
    headers: createSendHeaders(csrfHolder),
    body: JSON.stringify({ x, y }, null, 0),
    credentials: 'include',
  }));
  validateAnswer(answer);
  if (answer.status === 200) {
    return answer.json();
  }
  return null;
}

async function deleteElement(compoId, elementId, csrfHolder) {
  const answer = await checkAndUpdateCsrf(fetch(`${HOSTNAME}/api/v1/rest/compositions/${compoId}/elements/${elementId}`, {
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

async function doGetCompositions() {
  console.log('Check account');
  const res = await getMyself();
  if (res !== null) {
    console.warn('Test might be biased: user already authentified');
  }

  console.log('Login');
  const loginResult = await login('mem2@collamap.com', 'pwd-mem2');
  console.log('login achieved');

  console.log('Get compositions');
  const compositions = await getMyCompositions();
  console.log('User\'s compositions retrieved', compositions);

  if (!compositions.ownedCompositions.length) {
    throw new Error('The use should have at least one owned compositions');
  }
  const compoId = compositions.ownedCompositions[0].id;

  const composition = await getComposition(compoId);
  console.log('Composition retrieved', composition);

  console.log('Logout');
  const logoutRes = await logout();
  console.log('Logout achieved');

  return true;
}

async function doCreateEditDeleteComposition() {
  console.log('Check account');
  const res = await getMyself();
  if (res !== null) {
    console.warn('Test might be biased: user already authentified');
  }

  console.log('Login');
  const loginResult = await login('mem2@collamap.com', 'pwd-mem2');
  console.log('login achieved');

  console.log('Create composition');
  const composition = await createComposition('My new compo');
  console.log('Compo created', composition);

  console.log('Change composition title');
  let compositionEdit = await changeCompositionTitle(composition.id, 'Title updated');
  composition.title = compositionEdit.title;
  console.log('Compo title changed', compositionEdit.title);

  console.log('Change composition collaborative');
  compositionEdit = await changeCompositionCollaborative(composition.id, true);
  composition.collaborative = compositionEdit.collaborative;
  console.log('Compo collaborative changed', compositionEdit.collaborative);

  console.log('Delete composition');
  deleteRes = await deleteComposition(composition.id);
  console.log('Composition deleted');

  console.log('Logout');
  const logoutRes = await logout();
  console.log('Logout achieved', logoutRes);
}

async function doAddEditDeleteElements() {
  console.log('Check account');
  const res = await getMyself();
  if (res !== null) {
    console.warn('Test might be biased: user already authentified');
  }

  console.log('Login');
  const loginResult = await login('mem1@collamap.com', 'pwd-mem1');
  console.log('login achieved');

  try {
    console.log('Get compositions');
    const compositions = await getMyCompositions();
    console.log('User\'s compositions retrieved', compositions);

    if (!compositions.ownedCompositions.length) {
      throw new Error('The use should have at least one owned compositions');
    }
    const compoId = compositions.ownedCompositions.filter(c => !c.collaborative).map(c => c.id)[0];
    if (!compoId) {
      throw new Error('Test composition not found.');
    }

    console.log('Create element');
    const element = await createElement(compoId, 'rect', 20, 30, style = 'color: red;', extraProps = null)
    console.log('Element created', element);

    console.log('Check element present in composition');
    let composition = await getComposition(compoId);
    console.log('Composition retrieved', composition);
    if (!composition.elements.some((e) => e.id === element.id)) {
      throw new Error('Element not in composition')
    }

    console.log('Update element');
    element.elementType = 'circle'
    element.x = 120;
    element.y = 200;
    element.style = null;
    element.r = 30;
    const updatedElement = await updateElement(compoId, element);
    console.log('Element updated', updatedElement);

    console.log('Check element updated in composition');
    composition = await getComposition(compoId);
    console.log('Composition retrieved', composition);
    if (!composition.elements.some((e) => e.id === element.id
      && e.elementType === element.elementType && e.x === element.x
      && e.y === element.y && e.style === element.style && e.r === 30)) {
      throw new Error('Element not in composition')
    }

    console.log('Update element position');
    const position = await updateElementPosition(compoId, element.id, 300, 400);
    console.log('position updated', position);

    console.log('Delete element');
    const deleteRes = await deleteElement(compoId, element.id);
    console.log('element deleted', deleteRes);
  } finally {
    console.log('Logout');
    const logoutRes = await logout();
    console.log('Logout achieved');
  }

  return true;
}

async function doShareUrlTest() {
  let compoId;

  console.log('Check account');
  let res = await getMyself();
  if (res !== null) {
    console.warn('Test might be biased: user already authentified');
  }
  console.log('Login with MEM1');
  let user = await login('mem1@collamap.com', 'pwd-mem1');
  console.log('login achieved for MEM1', user);
  try {
    console.log('Create composition');
    const composition = await createComposition('My new compo');
    console.log('Compo created', composition);
    compoId = composition.id;
  } finally {
    console.log('Logout from MEM1');
    const logoutRes = await logout();
    console.log('Logout achieved');
  }

  console.log('Check account');
  res = await getMyself();
  if (res !== null) {
    console.warn('Test might be biased: user already authentified');
  }
  console.log('Login with MEM2');
  user = await login('mem2@collamap.com', 'pwd-mem2');
  console.log('login achieved for MEM2', user);
  try {
    console.log('Access compo from MEM2');
    const composition = await getComposition(compoId);
    console.log('Composition found', composition);

    console.log('Attempt forbidden modification that would be allowed in collaboration');
    try {
      console.log('Create element from MEM2');
      await createElement(compoId, 'rect', 20, 30, style = 'color: red;', extraProps = null)
      throw new Error('ELEMENTCREATED');
    } catch (e) {
      if (e.message === 'ELEMENTCREATED') {
        throw new Error('Element should not be created');
      }
      console.log('request forbidden as required.');
    }

  } finally {
    console.log('Logout from MEM2');
    const logoutRes = await logout();
    console.log('Logout achieved');
  }

  console.log('Check account');
  res = await getMyself();
  if (res !== null) {
    console.warn('Test might be biased: user already authentified');
  }
  console.log('Login with MEM1');
  const loginResult = await login('mem1@collamap.com', 'pwd-mem1');
  console.log('login achieved');
  try {
    console.log('Access compo from MEM1');
    const composition = await getComposition(compoId);
    console.log('Composition found', composition);
    if (!composition.guests.some((g) => g.email === 'mem2@collamap.com')) {
      throw new Error('Guest not found');
    }

    console.log('Delete composition');
    deleteRes = await deleteComposition(compoId);
    console.log('Composition deleted');
  } finally {
    console.log('Logout from MEM1');
    const logoutRes = await logout();
    console.log('Logout achieved');
  }
}

async function doTests() {
  console.log("START COMPOSITION TESTS");
  try {
    console.log("GET USERS COMPOSITION");
    await doGetCompositions();

    console.log("CREATE / DELETE COMPOSITION");
    await doCreateEditDeleteComposition();

    console.log("MANIPULATE ELEMENTS");
    await doAddEditDeleteElements();

    console.log("SHARE COMPOSITION");
    await doShareUrlTest();

    console.log("ALL COMPOSITION TESTS SUCCEEDED.");
  } catch (error) {
    console.warn('Error happend');
    console.error(error);
    console.log("COMPOSITION TESTS FAILED.");
  }
}

doTests();
