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

async function waitSeconds(seconds=3) {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(true);
    }, seconds * 1000);
  })
}

function sendCompositionOrder(wsClient, compoId, order) {
  wsClient.publish({destination: `/app/compositions.${compoId}`, body: JSON.stringify(order, null, 0)});
}

async function sendElementPositionChangedOrder(wsClient, compoId, elemId) {
  await waitSeconds(2);
  console.log('Send element change position');
  let order = {orderType: 'elementPositionChanged', elementId: elemId, x: 54, y: 89};
  sendCompositionOrder(wsClient, compoId, order);
}

async function sendElementDeletedOrder(wsClient, compoId, elemId) {
  await waitSeconds(2);
  console.log('Send element deletion');
  let order = {orderType: 'elementDeleted', elementId: elemId};
  sendCompositionOrder(wsClient, compoId, order);
}

function handleErrorQueueMessage(rawMessage, client, testSteps) {
  try {
    let message = JSON.parse(rawMessage);
    console.warn('ERROR queue message received', message);
  } catch (e) {
    console.warn(`Incoming ERROR queue message error: ${e.message}`);
  }
}

function handleCompositionTopicMessage(rawMessage, wsClient, testSteps) {
  try {
    let message = JSON.parse(rawMessage);
    console.log('Composition topic message received', message);
    if (message.orderType === 'MEMBER_JOINED' && message.email === 'mem2@collamap.com' && testSteps.step === 0) {
      console.log('other user connected in proper order');
      testSteps.step++;
    } else if (message.orderType === 'elementAdded' && message.authorEmail === 'mem2@collamap.com' && testSteps.step === 1) {
      console.log('other user add element in proper order');
      testSteps.step++;
      sendElementPositionChangedOrder(wsClient, message.compositionId, message.element.id);
    } else if (message.orderType === 'elementPositionChanged' && message.authorEmail === 'mem1@collamap.com' && testSteps.step === 2) {
      console.log('current user moved element in proper order');
      testSteps.step++;
    } else if (message.orderType === 'elementPositionChanged' && message.authorEmail === 'mem2@collamap.com' && testSteps.step === 3) {
      console.log('other user moved element in proper order');
      testSteps.step++;
      sendElementDeletedOrder(wsClient, message.compositionId, message.elementId);
    } else if (message.orderType === 'elementDeleted' && message.authorEmail === 'mem1@collamap.com' && testSteps.step === 4) {
      console.log('current user deleted element in proper order');
      testSteps.step++;
      waitSeconds(5).then(() => {
        testSteps.disconnect();
      });
    }
  } catch (e) {
    console.warn(`Incoming composition topic message error: ${e.message}`);
  }
}

function handleCompositionQueueMessage(rawMessage, client, testSteps) {
  try {
    let message = JSON.parse(rawMessage);
    console.log('Composition queue message received', message);
  } catch (e) {
    console.warn(`Incoming composition queue message error: ${e.message}`);
  }
}

async function doWebSocketTest() {
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

    console.log("get CSRF token");
    const { headerName, token } = await getCsrfToken();
    const stompHeader = { [headerName]: token };

    console.log("Setup websocket");
    const WSTestPromise = new Promise((resolve, reject) => {
      let promiseDone = false;
      const testSteps = {step: 0} //0: start, 1: guest connected 2: guest created an element, 3: I move the element, 4: guest moved the element, 4: I delete the element
      const client = new Client({
        webSocketFactory: () => new SockJS(`${HOSTNAME}/api/v1/websocket`),
        reconnectDelay: 0, // TODO: REMOVE
        // debug: (str) => console.log(str),
        connectHeaders: stompHeader,
        onConnect: async () => {
          console.log('Promise connected, subscribe to personal queue and compositioon topic');
          const errorQueueSub = client.subscribe('/user/queue/errors', (iMessage) => handleErrorQueueMessage(iMessage.body, client, testSteps));
          const compoQueueSub = client.subscribe('/user/queue/compositions', (iMessage) => handleCompositionQueueMessage(iMessage.body, client, testSteps));
          const compoTopicSub = client.subscribe(`/topic/compositions.${EDITED_COMPOSITION.id}`, (iMessage) => handleCompositionTopicMessage(iMessage.body, client, testSteps));
          
          testSteps.disconnect = () => {
            testSteps.autoDisconnect && clearTimeout(testSteps.autoDisconnect);
            testSteps.autoDisconnect = null;
            console.log('disconnect');
            compoTopicSub.unsubscribe();
            compoQueueSub.unsubscribe();
            errorQueueSub.unsubscribe();
            waitSeconds(2).then(() => {
              client.deactivate();
            });
          }

          console.log('prepare autodeconnection');
          testSteps.autoDisconnect = setTimeout(() => {
            console.log('Disconnect with failure');
            testSteps.failed = true;
            testSteps.disconnect();
          }, 20000);
        },
        onDisconnect: (iMessage) => {
          console.log('WS disconnected properly', iMessage);
          if (!promiseDone) {
            promiseDone = true;
            if (testSteps.failed) {
              reject('Ws disconnect but not all tests passed');
            } else {
              resolve('Ws disconnect');
            }
          }
        },
        onStompError: (iMessage) => {
          console.warn('STOMP ERROR', iMessage);
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
    await WSTestPromise;

  } finally {
    console.log('Logout from MEM1');
    const logoutRes = await logout();
    console.log('Logout achieved');
  }
}

async function doTests() {
  console.log("START WEBSOCKET 1 TESTS");
  try {
    await doWebSocketTest();
    console.log("ALL WEBSOCKET 1 TESTS SUCCEEDED.");
  } catch (error) {
    console.warn('Error happend');
    console.error(error);
    console.log("WEBSOCKET 1 TESTS FAILED.");
  }
}

doTests();