<p align="center">
 <img id="top" src="image.png" alt="description" width="200"/>
</p>
<h1 align="center">PROG7315 POE FlyingBudgie</h1>
<br>
<br>
<h2 align="center">Introduction</h2>
<p1 align="center">FlyingBudgie is designed as a real-time flight tracker, giving users the ability to monitor live flights, view aircraft paths, and access detailed information on aircrafts. This is achieved through the OpenSkyNetwork API and the Google Maps API. The app is aimed at those interested in monitoring aircrafts and do so in an interactive engaging way.
</p1>
<br><br>
<p align="center">
  James Hart | Weylin Volschenk | Kevin Muller | Jordan Muller
 <br>
 Youtube Link to Demonstrative Video for Part 3: <br>
https://youtu.be/7IpH9m6LG8k
<br>
 YouTube Video for Biometrics within Part 3 (Watch this Last): <br>
https://www.youtube.com/shorts/1A1g2C5xATs
<br>
<h2>Part 3 Update</h2>
 
<h3>Biometrics</h3>
The biometrics have been implemented when a user registers for the first time. After a successful registration the user is then prompted to user their fingerprint to show that the device is theirs and it is the intended user of the phone. Once a user is logged into the app, whenever they open the app again, they will log in automatically so there is no biometrics after this point.
 
<h3>Notifications</h3>
The live notifications happen by using androids WorkManager to run in the background and scheduled to run every 15 minutes to check, in a radius around the user’s last known location to see the flights in a 10km,50km and 100km. This meets the requirement of the notifications being real-time as it takes gets the current flights in the radius when the notification is called. The functionality can be checked manually using the button on the alerts page.

<h3>Flight Tracking (User Feature)</h3>
Part of the Features designed in part 1 was the flight inspection feature. This was implemented in Part 2 by selecting a plane that was shown on the Map. In part 3 it was expanded into showing Flight Paths of the aircraft from when the plane took off and the ADS-B started tracking. 

<h3>Playback Feature (User Feature)</h3>
As planned in Part 1, the Playback feature allows a user to take historical flights stored on FireStore and return them to the user to show what flights were flying where every hour. This data is compiled by the User as they use the application, more data is uploaded from the current api call to show in the Playback. 

<h3>Offline Mode </h3>
For Part 3 a offline Mode is required so that the application can operate offline and sync back up when the application regains network connectivity. This is implemented using the Playback Feature, where users can track historical flights. As live flight tracking cannot be done without internet connectivity, tracking previous flights using offline data was a logical choice. 

<h3>Multi-Language Support</h3>
Using the Built-In language support for Android, translating between languages is done through the settings page and toggle.  


 <br>
<h2>User Features</h2>
<p>
 
**Flight Tracking**
 
•	Allowing the user to track all the flights visible on the map with the ability to filter and select flights. 
 
**Flight Filters**

•	When viewing flights, the user will be able to filter the by categories such as Plane Type and Origin. The Departure and Arrival Filters allow users to see the number of flights passing through the airport over 24 hours. 

**Flight Searching**

•	The user will be able to search for flights using the Callsign of the plane they are looking for. 

**Flight Inspecting**

•	The application will allow users to look at detailed information of the plane like speed, altitude and their bearing. 

</p>

<h2>API's Used</h2>

- Google Maps API

- OpenSkyNetwork
 
<h2>Tools Used</h2>
<div>
  <a href="https://developer.android.com/studio" target="_blank">
    <img src="https://img.shields.io/badge/Android%20Studio-0096FF?style=for-the-badge&logo=android-studio&logoColor=white&width=200" alt="Android Studio Badge">
  </a>
  <br>
  <a href="https://github.com/" target="_blank">
    <img src="https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white&width=200" alt="GitHub Badge">
  </a>
  <br>
  <a href="https://kotlinlang.org/" target="_blank">
    <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white&width=200" alt="Kotlin Badge">
  </a>
  <br>
  
 <h2>Running the Project</h2>
  <p1 align="center"> To run the project, you can either clone the project or open it through android studio and clone it via github. 
</p1>
<br>
<p1>
  Before running the program you will need to sync the gradle files, to do this go to the top right of the screen and look for the elephant symbol with a diagonal arrow pointing down.
 <br>
  <img width="650" height="300" alt="image" src="https://github.com/user-attachments/assets/c73713b4-8210-46e6-94d7-7da83e8d1bcf" />
  <br>
  Once clicked please wait for the Gradle to sync with the project. 
 <br>
 Once that is complete, you must open the local.properties file inside of the ide and add in this line to allow for the google maps api to function
 <br>
 <br>
  MAPS_API_KEY=AIzaSyB2kLMqmnMXhYHfO8KOkDLiPKudBQJMW3k
 <br>
 <br>
 <img width="372" height="548" alt="image" src="https://github.com/user-attachments/assets/5d7df88c-e145-4544-910f-186255cb3a04" />
 <img width="646" height="257" alt="image" src="https://github.com/user-attachments/assets/a0380903-61c2-4170-94cc-51bbcd2ffb51" />
 <br>
</p1>
 You can now run the program using the green outlined arrow at the top-center of the IDE
 <br>
  <img width="650" height="300" alt="image" src="https://github.com/user-attachments/assets/f28dd2ec-16da-46b1-8a4e-f2d72444e3f1" />
 <br>
 

 <h2>Explaining UI</h2>
 
 <!--Login Page-->
 <table style="width:100%; table-layout: fixed;">
  <tr>
   <td>
   <img width="650" height="1750" alt="image" src="https://github.com/user-attachments/assets/805fd1a6-1bd9-4340-b98b-7de0a33c1cb1" />
   </td>
   <td>
    <strong>Login Page</strong><br>
The login screen is pretty standard as it takes an email and password. There is an option to login with and SSO using Google.
The login buttons take you to the main page.The Sign Up link at the top takes you to the Sign Up page.
   </td>
  </tr>
 </table>

 <!--Sign Up-->
 <table style="width:100%; table-layout: fixed;">
  <tr>
   <td>
  <img width="535" height="1750" alt="image" src="https://github.com/user-attachments/assets/7c20cd73-81d1-496a-a48c-e3a81b499533" />
   </td>
   <td>
     <strong>Sign Up</strong><br>
  
The sign up page is also pretty standard with the user’s name, email and password being captured.
The sign up button takes you to the main page.
The Login link takes you to the login page.
   </td>
  </tr>
 </table>

 <!--Main Page-->
 <table style="width:100%; table-layout: fixed;">
  <tr>
   <td>
 <img width="880" height="1750" alt="image" src="https://github.com/user-attachments/assets/aa04a4a7-ce69-4e7b-976b-25c5fe16aa83" />
   </td>
   <td>
     <strong>Main Page</strong><br>
The main page shows the world map with current flights. The user can search and filter flights at the top of the screen. The arrow button at the bottom right will locate the user on the map if it is clicked. The Playback navigation item will open the Playback screen. Same with the Alert item and Settings item.
   </td>
  </tr>
  
 </table>
   <!--Filter Page-->
 <table style="width:100%; table-layout: fixed;">
  <tr>
   <td>
 <img width="880" height="1750" alt="image" src="https://github.com/user-attachments/assets/66859e18-2df2-4a06-b37e-d1f34f7d6468" />
   </td>
   <td>
     <strong>Filter Functionality</strong><br>
    
The Filter Page shows the world map with current flights. The user can search and filter flights at the top of the screen. The arrow button at the bottom right will locate the user on the map if it is clicked. The Playback navigation item will open the Playback screen. Same with the Alert item and Settings item.
   </td>
  </tr>
 </table>

  <!--Search Page-->
 <table style="width:100%; table-layout: fixed;">
  <tr>
   <td>
 <img width="475" height="1750" alt="image" src="https://github.com/user-attachments/assets/18cd92a7-7ff8-4166-a5d4-de0460d23142" />

   </td>
   <td>
         <strong>Search Functionality</strong><br>
   
The search screen that will pop up when the search bar is tapped on the main screen. It is a basic search screen that takes a flight number and searches for it on the map.
   </td>
  </tr>
 </table>

 
  <!--Select Flight -->
 <table style="width:100%; table-layout: fixed;">
  <tr>
   <td>
 <img width="540" height="1750" alt="image" src="https://github.com/user-attachments/assets/b9b654d1-a568-4614-bb2a-7810a1d37b2d" />

   </td>
   <td>
    <strong>Inspect Functionality</strong><br>
This screen will be shown when a plane is tapped on the main screen. This page shows the information of the plane and the flight it is currently on with its speed, altitude and course heading.
   </td>
  </tr>
 </table>

   <!--Change to Satellite -->
 <table style="width:100%; table-layout: fixed;">
  <tr>
   <td>
    <img width="325" height="700" alt="image" src="https://github.com/user-attachments/assets/fbf6b6f9-c6e6-4b18-bffe-b64b512ef0b6" />

   </td>
   <td>
      <strong>Satellite Setting</strong><br>
In the settings page the user is able to change between having tha maps on street view or satellite view using the slider
   </td>
  </tr>
 </table>
  <!--Change Icon colour -->
 <table style="width:100%; table-layout: fixed;">
  <tr>
   <td>
<img width="375" height="700" alt="image" src="https://github.com/user-attachments/assets/26661573-99b2-4eb8-a1fa-674bffdf6909" />

   </td>
   <td>
     <strong>Icon Setting</strong><br>
The user will also be able to change the plane icon colour to better suit their preference. These colours are red, blue, yellow and purple.
   </td>
  </tr>
 </table>
</div>


  <h2>Contact</h2>
  James Hart - ST10256074@vcconnect.edu.za
  <br>
Weylin Volschenk - ST10390916@vcconnect.edu.za
  <br>
Kevin Muller - ST10355869@vcconnect.edu.za
  <br>
Jordan Muller - ST10150702@vcconnect.edu.za
  <br>
  <br>

<h2>AI Declaration and Usage</h2>
Tools used


• ChatGPT

• Gemini

• Microsoft Copilot

• Claude

<h3>AI Usage</h3>

AI has been used in the production of the application for the use of bug fixing, modifying icons, providing examples and learning how to use commands inside of Android Studio. ChatGPT was used to identify and fix coding errors and Gradle build issues. Microsoft Copilot was used to generate and customize plane icons. Claude was employed to locate the SHA-1 Certificate Hash required to enable Google Sign-In integration (Figure 7). Every tool’s output was read through and used as either an explanation or template in which to expand and understand the work. No work was directly copied and used in the production of the application.

AI Screenshots
Inside word file on ARC
References
Android Developor, 2025. Save simple data with SharedPreferences. [Online] Available at: https://developer.android.com/training/data-storage/shared-preferences [Accessed 6 October 2025].

CodingSTUFF, 2024. How to Change Google Map Type in Android Studio 2024 : All About Google Maps. [Online] Available at: https://www.youtube.com/watch?v=KVE1KWyLyn0 [Accessed 4 October 2025].

Google Maps Platform, 2025. Camera and view. [Online] Available at: https://developers.google.com/maps/documentation/android-sdk/views [Accessed 5 September 2025].

Google Maps Platform, 2025. Maps SDK for Android Quickstart. [Online] Available at: https://developers.google.com/maps/documentation/android-sdk/start [Accessed 7 September 2025].

Google Maps Platform, 2025. Markers. [Online] Available at: https://developers.google.com/maps/documentation/android-sdk/marker [Accessed 12 September 2025].

Google Maps Platform, 2025. Request location permissions. [Online] Available at: https://developer.android.com/develop/sensors-and-location/location/permissions [Accessed 21 September 2025].

Google Maps Platform, 2025. Set up the Maps SDK for Android. [Online] Available at: https://developers.google.com/maps/documentation/android-sdk/get-api-key [Accessed 4 September 2025].

json2kt, 2025. JSON to Kotlin Data Class Generator Online. [Online] Available at: https://json2kt.com/ [Accessed 12 September 2025].

Kotlin, 2025. Map. [Online] Available at: https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-map/ [Accessed 2 October 2025].

Microsoft, 2025. https://copilot.microsoft.com/. [Online] Available at: https://copilot.microsoft.com/shares/Pms2D4LD1yS2Ugp8HBuM9 [Accessed 6 October 2025].

Photopea, 2025. Photopea Home. [Online] Available at: https://www.photopea.com/ [Accessed 6 October 2025].

removebg, 2025. Remove Image Background. [Online] Available at: https://www.remove.bg/upload [Accessed 6 October 2025].

The OpenSky Network, 2025. OpenSky REST API. [Online] Available at: https://openskynetwork.github.io/opensky-api/rest.html [Accessed 3 August 2025].

Android Developers, 2025. androidx.biometric. [Online] 
Available at: https://developer.android.com/reference/kotlin/androidx/biometric/package-summary
[Accessed 15 November 2025].

aayushitated2000, 2025. How to Push Notification in Android using Firebase Cloud Messaging?. [Online] 
Available at: https://www.geeksforgeeks.org/android/how-to-push-notification-in-android-using-firebase-cloud-messaging/
[Accessed 17 November 2025].

Android Developers, 2025. BiometricManager.Authenticators. [Online] 
Available at: https://developer.android.com/reference/kotlin/androidx/biometric/BiometricManager.Authenticators
[Accessed 15 November 2025].

Android Developers, 2025. Create a notification. [Online] 
Available at: https://developer.android.com/develop/ui/views/notifications/build-notification
[Accessed 17 November 2025].

Android Developers, 2025. Define work requests. [Online] 
Available at: https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work
[Accessed 18 November 2025].

Android Developers, 2025. Getting started with WorkManager. [Online] 
Available at: https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started
[Accessed 17 November 2025].

Android Developers, 2025. Task scheduling. [Online] 
Available at: https://developer.android.com/develop/background-work/background-tasks/persistent
[Accessed 18 November 2025].

iamabhijha, 2025. How to Integrate Work Manager in Android?. [Online] 
Available at: https://www.geeksforgeeks.org/kotlin/how-to-integrate-work-manager-in-android/
[Accessed 18 November 2025].

introidx, 2025. How to Add Fingerprint Authentication in Your Android App without Using any Library?. [Online] 
Available at: https://www.geeksforgeeks.org/android/how-to-add-fingerprint-authentication-in-your-android-app-without-using-any-library/
[Accessed 15 November 2025].

Movable Type Scripts, 2025. Calculate distance, bearing and more between Latitude/Longitude points. [Online] 
Available at: https://www.movable-type.co.uk/scripts/latlong.html
[Accessed 18 November 2025].

The OpenSky Network, 2025. OpenSky REST API. [Online] 
Available at: https://openskynetwork.github.io/opensky-api/rest.html
[Accessed 3 August 2025].

WALA, I., 2023. ANDROID - BIOMETRIC FINGER-PRINT AUTHENTICATION || TUTORIAL IN KOTLIN. [Online] 
Available at: https://www.youtube.com/watch?v=p91qEmVj8LQ
[Accessed 15 November 2025].


