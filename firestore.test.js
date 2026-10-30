const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read shops", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("shops").get());
});

test("Authenticated user: can read shops and products", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("shops").get());
  await assertSucceeds(aliceDb.collection("products").get());
});

test("Authenticated user: can create pending shop registration with valid data", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("shops").doc("shop_1").set({
      id: "shop_1",
      name: "Sharma General Store",
      category: "Grocery",
      status: "pending",
      isOpen: false,
      ownerId: ALICE_UID,
    })
  );
});

test("Authenticated user: can create product with valid data", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("products").doc("prod_1").set({
      id: "prod_1",
      shopId: "shop_1",
      name: "Aashirvaad Atta (5kg)",
      price: 275,
      inStock: true,
    })
  );
});

test("Unauthenticated user: cannot create or read orders", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("orders").doc("order_unauth").set({
      id: "order_unauth",
      userId: "guest",
      shopId: "shop_1",
      totalAmount: 314,
      status: "pending",
    })
  );
  await assertFails(unauthDb.collection("orders").doc("order_1").get());
});

test("Authenticated user: can create order for self and read own order", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("orders").doc("order_1").set({
      id: "order_1",
      userId: ALICE_UID,
      shopId: "shop_1",
      totalAmount: 314,
      status: "pending",
    })
  );
  await assertSucceeds(aliceDb.collection("orders").doc("order_1").get());
});

test("Cross-user isolation: user cannot create order pretending to be another user or read another user's private order", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("orders").doc("order_alice_1").set({
      id: "order_alice_1",
      userId: ALICE_UID,
      shopId: "shop_1",
      totalAmount: 314,
      status: "pending",
    })
  );

  // Bob cannot read Alice's private order
  await assertFails(bobDb.collection("orders").doc("order_alice_1").get());

  // Bob cannot create an order with Alice's userId
  await assertFails(
    bobDb.collection("orders").doc("order_2").set({
      id: "order_2",
      userId: ALICE_UID, // Bob is authenticated, but sets userId to Alice
      shopId: "shop_1",
      totalAmount: 314,
      status: "pending",
    })
  );
});

test("User profile: Alice can write her own profile, Bob cannot write Alice's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      id: ALICE_UID,
      role: "customer",
    })
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).set({
      id: ALICE_UID,
      role: "admin",
    })
  );
});
