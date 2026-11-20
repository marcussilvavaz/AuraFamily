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
const CHARLIE_UID = "charlie_789";
const HOUSEHOLD_ID = "FAM_ALICE123";

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

async function seedAliceHousehold() {
  const now = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Silva",
      householdId: HOUSEHOLD_ID,
      themeMode: "SYSTEM",
      baseCurrency: "BRL",
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("households").doc(HOUSEHOLD_ID).set({
      householdId: HOUSEHOLD_ID,
      name: "Família Silva",
      ownerId: ALICE_UID,
      monthlyIncomeBrl: 12000.0,
      needsPercentage: 50,
      wantsPercentage: 30,
      savingsPercentage: 20,
      createdAt: now,
      updatedAt: now,
    });
  });
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("households").doc(HOUSEHOLD_ID).collection("tasks").get());
  await assertFails(unauthDb.collection("households").doc(HOUSEHOLD_ID).collection("transactions").get());
  await assertFails(
    unauthDb.collection("households").doc(HOUSEHOLD_ID).collection("tasks").doc("task_1").set({
      userId: ALICE_UID,
      householdId: HOUSEHOLD_ID,
      title: "Pagar luz",
      notes: "",
      category: "Finanças",
      priority: "HIGH",
      dateEpochDay: 20777,
      yearMonth: "2026-11",
      alarmHour: 9,
      alarmMinute: 0,
      alarmEnabled: true,
      completed: false,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("UserProfile and Household creation, updates, and family member sharing work as expected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Alice creates her user profile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Silva",
      householdId: HOUSEHOLD_ID,
      themeMode: "DARK",
      baseCurrency: "BRL",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice can read her household doc even before it exists (returns empty snapshot without PERMISSION_DENIED)
  await assertSucceeds(aliceDb.collection("households").doc(HOUSEHOLD_ID).get());

  // Alice creates the household
  await assertSucceeds(
    aliceDb.collection("households").doc(HOUSEHOLD_ID).set({
      householdId: HOUSEHOLD_ID,
      name: "Família Silva",
      ownerId: ALICE_UID,
      monthlyIncomeBrl: 15000.0,
      needsPercentage: 50,
      wantsPercentage: 30,
      savingsPercentage: 20,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject invalid 50/30/20 sum (e.g. 60+30+20 = 110)
  await assertFails(
    aliceDb.collection("households").doc(HOUSEHOLD_ID).update({
      needsPercentage: 60,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob joins Alice's household by setting his householdId
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Roberto Silva",
      householdId: HOUSEHOLD_ID,
      themeMode: "LIGHT",
      baseCurrency: "BRL",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob can now read and update the shared household budget
  await assertSucceeds(bobDb.collection("households").doc(HOUSEHOLD_ID).get());
  await assertSucceeds(
    bobDb.collection("households").doc(HOUSEHOLD_ID).update({
      monthlyIncomeBrl: 16500.0,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Charlie (non-member) cannot read or update Alice's household
  await assertFails(charlieDb.collection("households").doc(HOUSEHOLD_ID).get());
  await assertFails(
    charlieDb.collection("households").doc(HOUSEHOLD_ID).update({
      monthlyIncomeBrl: 100.0,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Tasks CRUD, completion checkbox update, and list on empty and populated household subcollection", async () => {
  await seedAliceHousehold();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = new Date(Date.now() - 2000);

  const tasksRef = aliceDb.collection("households").doc(HOUSEHOLD_ID).collection("tasks");

  // Listing empty subcollection succeeds immediately for household member
  await assertSucceeds(tasksRef.get());

  await assertSucceeds(
    tasksRef.doc("task_1").set({
      userId: ALICE_UID,
      householdId: HOUSEHOLD_ID,
      title: "Revisar fatura do cartão",
      notes: "Verificar compras em USD",
      category: "Finanças",
      priority: "HIGH",
      dateEpochDay: 20777,
      yearMonth: "2026-11",
      alarmHour: 14,
      alarmMinute: 30,
      alarmEnabled: true,
      completed: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Check task as completed (partial update)
  await assertSucceeds(
    tasksRef.doc("task_1").update({
      completed: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Listing populated subcollection succeeds
  await assertSucceeds(tasksRef.get());

  // Non-member query fails
  await assertFails(
    charlieDb.collection("households").doc(HOUSEHOLD_ID).collection("tasks").get()
  );

  // Extra @DocumentId 'id' key is rejected
  await assertFails(
    tasksRef.doc("task_2").set({
      id: "task_2",
      userId: ALICE_UID,
      householdId: HOUSEHOLD_ID,
      title: "Invalid ID field",
      notes: "",
      category: "Casa",
      priority: "LOW",
      dateEpochDay: 20777,
      yearMonth: "2026-11",
      alarmHour: 10,
      alarmMinute: 0,
      alarmEnabled: false,
      completed: false,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Transactions, Subscriptions, and SavingsGoals CRUD and subcollection list alignment", async () => {
  await seedAliceHousehold();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = new Date(Date.now() - 2000);

  const houseRef = aliceDb.collection("households").doc(HOUSEHOLD_ID);

  // Empty list queries succeed
  await assertSucceeds(houseRef.collection("transactions").get());
  await assertSucceeds(houseRef.collection("subscriptions").get());
  await assertSucceeds(houseRef.collection("savings_goals").get());

  // Create multi-currency expense transaction (USD converted to BRL)
  await assertSucceeds(
    houseRef.collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      householdId: HOUSEHOLD_ID,
      title: "Software Cloud USD",
      amountOriginal: 20.0,
      currency: "USD",
      exchangeRateToBrl: 5.8,
      amountBrl: 116.0,
      type: "EXPENSE",
      budgetBucket: "WANTS",
      category: "Tecnologia",
      dateEpochDay: 20777,
      yearMonth: "2026-11",
      recurringSourceId: "",
      createdByName: "Alice Silva",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(houseRef.collection("transactions").get());

  // Create recurring subscription bill
  await assertSucceeds(
    houseRef.collection("subscriptions").doc("sub_1").set({
      userId: ALICE_UID,
      householdId: HOUSEHOLD_ID,
      title: "Netflix Família",
      amountOriginal: 59.9,
      currency: "BRL",
      exchangeRateToBrl: 1.0,
      amountBrl: 59.9,
      category: "Streaming",
      budgetBucket: "WANTS",
      dueDayOfMonth: 15,
      reminderDaysBefore: 3,
      autoPostEnabled: true,
      lastPostedYearMonth: "2026-11",
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(houseRef.collection("subscriptions").get());

  // Create custom family savings goal
  await assertSucceeds(
    houseRef.collection("savings_goals").doc("goal_1").set({
      userId: ALICE_UID,
      householdId: HOUSEHOLD_ID,
      title: "Reserva Familiar de Emergência",
      targetAmountBrl: 30000.0,
      currentAmountBrl: 8500.0,
      incomePercentage: 15,
      category: "Reserva",
      targetYearMonth: "2027-06",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    houseRef.collection("savings_goals").doc("goal_1").update({
      currentAmountBrl: 9500.0,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(houseRef.collection("savings_goals").get());

  // Cross-user isolation on subcollections
  const charlieHouseRef = charlieDb.collection("households").doc(HOUSEHOLD_ID);
  await assertFails(charlieHouseRef.collection("transactions").get());
  await assertFails(charlieHouseRef.collection("subscriptions").get());
  await assertFails(charlieHouseRef.collection("savings_goals").get());
});
