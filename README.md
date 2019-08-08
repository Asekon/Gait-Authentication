# Gait Authentication
This application was my bachelor project. The idea behind it is the theory that Gait (walking pattern) can be a unique identifier among users the same way fingerprints are.

# How to run

Build the application and run it on an Android smartphone using Android Studio.


Save your Gait in the local database
```
1-Enter your name
2-press "Sign up" and put the phone in your pocket.
3-keep walking in a natural and straight manner until the application makes a notification sound.
4-If for any reason the user does not walk properly, the session must be restarted. ( Entering a user name that already exists while signing up will overwrite the existing profile)
```

Attempt to login using your Gait
```
1-Enter your name
2-press "Log in" and put the phone in your pocket.
3-keep walking in a natural and straight manner until the application makes a notification sound.
4-The application will either accept or deny the claimed user depending on whether his current Gait is similar to the one saved in the signing up process for this specific user. ( The Ratio is the similarity metric, the Lower the ratio the more similar the Gait. < 1 being ideal while 1.3 being acceptable)
```

Graphs
```
The graphs were just for illustration. The 2 graphs on the left are for the entire walking session(sign in and sign up). While the 2 on the right are a single cycle. Their sole purpose was debugging and illustration.
```
