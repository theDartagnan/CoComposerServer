import { Client } from '@stomp/stompjs';

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

async function getCsrfToken(csrfHolder) {
  const answer = await checkAndUpdateCsrf(fetch(`${HOSTNAME}/api/v1/rest/csrf`, {
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

async function waitSeconds(seconds = 3) {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(true);
    }, seconds * 1000);
  })
}

function handleCompositionTopicMessage(rawMessage) {
  try {
    let message = JSON.parse(rawMessage);
    console.log('Composition topic message received', message);
  } catch (e) {
    console.warn(`Incoming composition topic message error: ${e.message}`);
  }
}

async function startWSAndTestSubscription(topicDest, shouldFail = false) {
  console.log("get CSRF token");
  const { headerName, token } = await getCsrfToken();
  const stompHeader = { [headerName]: token };

  return new Promise((resolve, reject) => {
    let promiseDone = false;
    const client = new Client({
      webSocketFactory: () => new SockJS(`${HOSTNAME}/api/v1/websocket`),
      reconnectDelay: 0, // TODO: REMOVE
      // debug: (str) => console.log(str),
      connectHeaders: stompHeader,
      onConnect: async () => {
        console.log('Promise connected, subscribe to personal queue and compositioon topic');
        // `/topic/compositions.${EDITED_COMPOSITION.id}`
        const compoTopicSub = client.subscribe(topicDest, (iMessage) => handleCompositionTopicMessage(iMessage.body));

        console.log('Subscription done. Wait 1 sec and disconnect');
        await waitSeconds(1);
        compoTopicSub.unsubscribe();
        waitSeconds(1).then(() => {
          client.deactivate();
        });

      },
      onDisconnect: (iMessage) => {
        console.log('WS disconnected properly. Not expected', iMessage);
        if (!promiseDone) {
          promiseDone = true;
          if (shouldFail) {
            reject('Ws disconnect but should fail');
          } else {
            resolve('Ws disconnect');
          }
        }
      },
      onStompError: (iMessage) => {
        console.warn('STOMP ERROR', iMessage);
        if (iMessage.command === 'ERROR'
          && iMessage.headers?.message?.startsWith('Failed to send message to ExecutorSubscribableChannel')) {
          if (shouldFail) {
            promiseDone = true;
            resolve('Ws disconnect');
          }
        }
        // the client will disconnect after this
      },
      onWebSocketClose: (evt) => {
        console.log('Websocket closed', evt);
        if (!promiseDone) {
          promiseDone = true;
          reject('Weboscket close');
        }
      },
      onWebSocketError: (evt) => {
        console.warn('WEBSOCKET ERROR', evt);
      },
    });
    client.activate();
  });
}

async function doWebAllowedCompositionTopicSubSocketTest() {
  console.log('Check account');
  let res = await getMyself();
  if (res !== null) {
    console.warn('Test might be biased: user already authentified');
  }
  console.log('Login with MEM1');
  let user = await login('mem1@collamap.com', 'pwd-mem1');
  console.log('login achieved for MEM1', user);

  try {
    console.log('Get compositions');
    const compositions = await getMyCompositions();
    console.log('User\'s compositions retrieved', compositions);
    const EDITED_COMPOSITION = compositions.ownedCompositions.find((c) => c.title === 'Compo Member 1 - 2');
    if (!EDITED_COMPOSITION) {
      throw new Error('Composition not found');
    }

    await startWSAndTestSubscription(`/topic/compositions.${EDITED_COMPOSITION.id}`, false);

  } finally {
    console.log('Logout from MEM1');
    const logoutRes = await logout();
    console.log('Logout achieved');
  }
}

async function doWebUnknownCompositionTopicSubSocketTest() {
  console.log('Check account');
  let res = await getMyself();
  if (res !== null) {
    console.warn('Test might be biased: user already authentified');
  }
  console.log('Login with MEM1');
  let user = await login('mem1@collamap.com', 'pwd-mem1');
  console.log('login achieved for MEM1', user);

  try {
    await startWSAndTestSubscription(`/topic/compositions.babababa`, true);
  } finally {
    console.log('Logout from MEM1');
    const logoutRes = await logout();
    console.log('Logout achieved');
  }
}

async function doWebPersonnalCompositionTopicSubSocketTest() {
  console.log('Check account');
  let res = await getMyself();
  if (res !== null) {
    console.warn('Test might be biased: user already authentified');
  }
  console.log('Login with MEM1');
  let user = await login('mem1@collamap.com', 'pwd-mem1');
  console.log('login achieved for MEM1', user);

  try {
    console.log('Get compositions');
    const compositions = await getMyCompositions();
    console.log('User\'s compositions retrieved', compositions);
    const EDITED_COMPOSITION = compositions.ownedCompositions.find((c) => !c.collaborative);
    if (!EDITED_COMPOSITION) {
      throw new Error('Composition not found');
    }

    await startWSAndTestSubscription(`/topic/compositions.${EDITED_COMPOSITION.id}`, true);
  } finally {
    console.log('Logout from MEM1');
    const logoutRes = await logout();
    console.log('Logout achieved');
  }
}

async function doWebCollaborativeompositionNotGuestTopicSubSocketTest() {
  console.log('MEM2: Check account');
  let res = await getMyself();
  if (res !== null) {
    console.warn('Test might be biased: user already authentified');
  }
  console.log('Login with MEM2');
  let user = await login('mem2@collamap.com', 'pwd-mem2');
  console.log('login achieved for MEM2');

  let compositions;
  try {
    console.log('Get MEM2 compositions');
    compositions = await getMyCompositions();
    console.log('User\'s compositions retrieved', compositions);
  } finally {
    console.log('Logout from MEM2');
    const logoutRes = await logout();
    console.log('Logout achieved');
  }

  const EDITED_COMPOSITION = compositions.ownedCompositions.find((c) => c.collaborative);
  if (!EDITED_COMPOSITION) {
    throw new Error('Composition not found');
  }

  console.log('MEM1: Check account');
  res = await getMyself();
  if (res !== null) {
    console.warn('Test might be biased: user already authentified');
  }
  console.log('Login with MEM1');
  user = await login('mem1@collamap.com', 'pwd-mem1');
  console.log('login achieved for MEM2');

  try {
    await startWSAndTestSubscription(`/topic/compositions.${EDITED_COMPOSITION.id}`, true);
  } finally {
    console.log('Logout from MEM1');
    const logoutRes = await logout();
    console.log('Logout achieved');
  }
}

async function doTests() {
  console.log("START WEBSOCKET AUTH TESTS");
  try {
    //await doWebAllowedCompositionTopicSubSocketTest();
    //await doWebUnknownCompositionTopicSubSocketTest();
    //await doWebPersonnalCompositionTopicSubSocketTest();
    await doWebCollaborativeompositionNotGuestTopicSubSocketTest();
    console.log("ALL WEBSOCKET AUTH TESTS SUCCEEDED.");
  } catch (error) {
    console.warn('Error happend');
    console.error(error);
    console.log("WEBSOCKET AUTH TESTS FAILED.");
  }
}

doTests();
