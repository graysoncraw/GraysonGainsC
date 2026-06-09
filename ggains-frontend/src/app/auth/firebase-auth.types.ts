export interface FirebaseWebConfig {
  apiKey: string;
  authDomain: string;
  projectId: string;
  appId: string;
}

export interface AuthSnapshot {
  email: string;
  firebaseUid: string;
  idToken: string;
  displayName: string;
}
