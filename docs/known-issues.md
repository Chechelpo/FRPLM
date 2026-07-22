Known, yet to fix, bugs: 
 - Response generation of existing messages must lower the tick of the session as well as disable the corresponding message
 - There's some weird issue with moving characters around. Currently it applies the movement yet for some reason later undoes it.
   - Found possible culprit: LocationEventsReactor#onNewResponseRegisterLocation being called after extensions run, effectively overriding location changes
    of EngineHolder#regenerate