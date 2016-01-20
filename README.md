# navigator-repro

A basic UI for adding, removing and editing items in a list, using React Native.

It should present a `ListView` with existing items which may be
focused or deleted.  New objects may be added to the list.

Selecting an item, or creating a new item, should switch to a screen
with the title of the item and a back button.

## Usage

```
lein cljsbuild once
natal xcode
```

Run the project in Xcode.

```
natal repl
```

## License

Copyright © 2016 Hugo Duncan

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
