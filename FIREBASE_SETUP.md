# Firebase setup

The source is Firebase-ready, but Firebase credentials are deliberately not
included in version control. Complete these steps once for the team project:

1. In the Firebase Console, create an Android app with package name
   `com.example.valentinesgarage`.
2. Download `google-services.json` and place it in `app/google-services.json`.
3. Enable **Authentication > Email/Password**.
4. Create a Cloud Firestore database. For development, start in test mode only
   while the team is setting it up; before a demonstration, apply role-based
   Firestore rules.
5. Create profile documents in the `users` collection using each Firebase
   Authentication UID as the document ID:

```json
{
  "displayName": "Valentine",
  "role": "MANAGER"
}
```

Use `"MECHANIC"` for mechanic accounts. Roles must be uppercase and match the
values in `UserRole`.

## Firestore collections

- `users`: display name and role
- `vehicles`: active truck status
- `checkIns`: immutable intake evidence
- `repairTasks`: shared checklist items and notes
- `activityLogs`: append-only manager audit trail

## Example development rules

These rules are a starting point. Tighten them for production by validating
field ownership and limiting manager-only writes.

```text
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    function signedIn() { return request.auth != null; }
    match /users/{userId} { allow read: if signedIn(); allow write: if request.auth.uid == userId; }
    match /{collection}/{document=**} { allow read, write: if signedIn(); }
  }
}
```
