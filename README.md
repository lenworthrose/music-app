# music-app

muZak is a music player for Android. It allows playback of content located on your device, and renders content from
the device's MediaStore.

muZak centers around a dynamically generated Playing Now playlist. You can view this playlist from the Playing Now
page, and while browsing your music library you can add tracks to this list. From the Playing Now page you can edit
the Playing Now playlist, using a combination of drag-and-drop and swipe to delete.

## Installation

muZak was developed in Android Studio, and includes the project files required to open it. Generating an APK should
be as easy and downloading the code, opening the project in Android Studio, and running the "assemble" gradle task.

## What It Does

When first launched, muZak will start a background service to retrieve all of the artists from the MediaStore,
adding them to the app's database. Then, it will query MediaStore's Albums table for cover art, which will be used
when viewing the list of artists in grid format. Then, it will fetch biography information and an artist photo from
Last.fm. I'd strongly recommend connecting to Wi-Fi for this!

## Contributing

1. Fork it!
2. Create your feature branch: `git checkout -b my-new-feature`
3. Commit your changes: `git commit -am 'Add some feature'`
4. Push to the branch: `git push origin my-new-feature`
5. Submit a pull request :D

## Credits

Written by Lenworth Rose

## License

Copyright 2015 Lenworth Rose

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
