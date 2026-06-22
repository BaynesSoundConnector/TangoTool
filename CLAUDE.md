# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Tango Playlist Tool** - A Java Swing desktop application for managing and playing tango music. The application organizes music into a hierarchical structure of Tracks, Tandas (collections of tracks), and Playlists. Users can search, organize, and play music with drag-and-drop functionality between different tree views.

**Language:** Java 17  
**Build System:** Eclipse IDE (no Gradle/Maven)  
**UI Framework:** Swing  
**Data Format:** XML (custom format for persistence)  
**External Dependencies:** XStream (XML serialization), Apache Commons Text (string similarity)

## Directory Structure

```
TTNew/
├── src/pkgForTTNew/          # Main source code (28 Java files)
├── bin/pkgForTTNew/          # Compiled .class files
└── .settings/                # Eclipse configuration
```

## Build & Run

**Compilation:** Eclipse IDE only — Project > Clean to rebuild. Java 17 (see `.settings/org.eclipse.jdt.core.prefs`). Classes output to `bin/pkgForTTNew/`.

**Run:**
```
java -cp bin pkgForTTNew.MainForTTNew
```

Entry point: `MainForTTNew.main()` creates the window and initializes all three data models.

## Architecture

### Core Data Models

**Track** (Track.java)
- Represents a single music file with metadata (title, orchestra, style, mood, danceability, rating, etc.)
- Enums: `Style` (Tango, Valse, Milonga, Cortina, AltTango, WCS, Salsa, Swing, Candombe, Other, Unknown), `Mood`, `Danceability`, `Status`, `Type` (WAV, MP3, MP4)
- Key fields: `uniqueId`, `fileName`, `relativePath`, `calculatedTime`, `inTandas` (list of tanda UIDs containing this track)

**Tanda** (Tanda.java)
- A collection of tracks (typically 3–5 for tango dancing); contains metadata: orchestra, description, source, style
- Tracks stored as `ArrayList<Long>` of track UIDs; reordered via `moveTrack()`, `insertTrack()`, `deleteTrack()`
- Change tracking via `bChanged` flag

**Playlist** (Playlist.java)
- Container for tandas; stores tanda IDs as `ArrayList<Long>`
- Tracks playback state: `currentlyPlayingTanda`, `currentlyPlayingTrack`

### Data Management Models

**TrackTableModel** (TrackTableModel.java)
- Extends `AbstractTableModel`; backs the track JTable
- Central track repository: `ArrayList<Track> mTracks`, `HashMap<Long, Track> hmTracks`
- Columns: Title, Orchestra, Style, Rating, Time, Album, UID, Tandas
- Search via Levenshtein distance matching; UID generation and lookup

**TandaTreeModel** (TandaTreeModel.java)
- Implements `TreeModel`; backs the Tanda JTree
- Tree structure: root > Style nodes (Tango, Valse, Milonga, Other) > Tandas > Tracks
- Central tanda repository: `ArrayList<Tanda> mTandas`
- `trackBeingShown` field filters tree to show only tandas containing a specific track

**PlaylistTreeModel** (PlaylistTreeModel.java)
- Implements `TreeModel` and `TreeModelListener`; backs the Playlist JTree
- Tree structure: root > Playlists > Tandas > Tracks
- Central playlist repository: `ArrayList<Playlist> mPlaylists`
- `showOnlyThisPlaylist` field filters to a single playlist view

**Singleton** (Singleton.java)
- Provides global access to all three models (`TrackTableModel`, `TandaTreeModel`, `PlaylistTreeModel`)
- Alternative to passing model references; set once at startup, read elsewhere

### Data Persistence

**DataHandler** (DataHandler.java)
- Two entry points: `loadData()` (legacy format) and `loadDataNew()` (new XML format)
- `loadData()` reads `database.xml` (XStream-serialized tracks + tandas + playlists in one file)
- `loadDataNew()` reads `tracks.xml` (new format via `TrackReader`) + `database.xml` for tandas/playlists
- Controlled by `MainForTTNew.NEWTRACKDATABASE` boolean flag

**TrackXMLWriter** (TrackXMLWriter.java)
- Writes tracks to the new XML format (`tracks.xml`)
- `writeTracksToXml()` is the current method; `writeTracksToXml_old()` is deprecated

**TrackReader** (TrackReader.java)
- SAX-based parser for reading the new `tracks.xml` format efficiently

**AssembleOldData** (AssembleOldData.java)
- Handles migration from the legacy data format (`music.xml` + playlist files)
- Used for one-time data import, not normal operation

**Database** (Database.java)
- Legacy data reading utilities; backup creation with timestamps

**Utilities** (Utilities.java)
- Static helpers: time formatting, validation, XML/string conversions, audio playback, `msg()`/`out()` for logging

### XML Tag Reference

- Legacy track tag: `<TT2.Track>` / `</TT2.Track>`
- New track tag: `<pkgForTTNew.Track>` / `</pkgForTTNew.Track>`
- DataHandler handles both when reading; always writes new format

### UI & Event Handling

**MainForTTNew** (MainForTTNew.java)
- Main `JFrame`; implements ActionListener, MouseListener, LineListener, ListSelectionListener, TableModelListener, ItemListener, TreeModelListener
- Three-panel layout: Track table (left), Tanda tree (middle), Playlist tree (right) via `JSplitPane`
- Menu: File > Add Track, Add Folder, Open, Save, Exit, Preferences, Validate
- UI state (divider positions) persisted to `%APPDATA%\TangoToolNew\config.txt`
- Manages single `Clip` instance for audio playback

**MouseEvents** (MouseEvents.java)
- Centralized mouse event handler for all three trees/tables
- Right-click context menus; double-click triggers play or edit dialogs

**TangoTransferHandler** (TangoTransferHandler.java)
- Drag-and-drop across trees and table via `SourceTarget` enum identifying node types
- COPY: track table → tanda; COPY_OR_MOVE: tracks within a tanda; MOVE: within playlists
- Style nodes and root nodes return `-1` from `getSourceActions()` to block dragging

**PlaylistTreeCellRenderer** (PlaylistTreeCellRenderer.java)
- Custom cell renderer for the playlist tree; handles visual highlighting of playing tracks

**Dialog Classes**
- `TrackDetailDialog.java` — add/edit track metadata
- `TandaDetailDialog.java` — create/edit tanda
- `TrackDialog.java` — simple track selection
- `AddFolderDialog.java` — bulk import tracks from a folder
- `PreferencesDialog.java` — user preferences (mostly unused)

### Export

**ExportXSPF** (ExportXSPF.java)
- Exports a playlist as XSPF (VLC-compatible XML playlist format)
- Handles character translation for special/accented characters in file paths

**ExportCD** (ExportCD.java)
- Creates a CD image directory structure from a playlist (copies tracks into `playlists\CDImages\<name>\disk1\`, etc.)

### Search

**SearchTermBuilder** (SearchTermBuilder.java)
- Normalizes search terms: strips accents (NFD decomposition), lowercases, concatenates title + orchestra
- Use `SearchTermBuilder.buildSearchTerm()` — replaces the deprecated `Orchestras.createSearchTerm()`

**Orchestras** (Orchestras.java)
- `createSearchTerm()` is superseded by `SearchTermBuilder.buildSearchTerm()`
- `normalizeOrchestraName()` is broken/deprecated (returns "Dont use this")

### Playback & Audio

**Player** (Player.java)
- `SwingWorker`-based background audio loader using `javax.sound.sampled.Clip`

**Playback Flow:**
1. User selects track and clicks Play (or playlist advances automatically)
2. `MainForTTNew.play(trackUID)` called
3. `Utilities.play()` loads audio via `Clip`
4. `Clip` fires `LineListener` events (START, STOP)
5. `MainForTTNew.update(LineEvent)` handles STOP to advance to next track

### Data Flow at Startup

1. `MainForTTNew()` constructor loads config, creates three model instances, calls `DataHandler.loadData()` or `loadDataNew()`
2. Models are cross-referenced: `TrackTableModel` → `TandaTreeModel` → `PlaylistTreeModel`
3. `Singleton` is populated with all three models
4. `TangoTransferHandler` shared across all components

### Critical Patterns

**Change Tracking:** All three models expose `isChanged()`/`setChanged()`. On exit, `saveDatabases()` checks all three and prompts to save. Backup creates timestamped `.bak` files before writing.

**Tree Model Events:** Structural changes call `notifyTreeModelHasChanged()` which fires `TreeModelEvent` to registered listeners.

**UID-Based References:** All Tanda→Track and Playlist→Tanda relationships use `long` UIDs. `TrackTableModel.hmTracks` and `TandaTreeModel` provide O(1) lookup by UID.

## Configuration & Persistence

**Config File:** `%APPDATA%\TangoToolNew\config.txt` — tab-separated key/value pairs
- Keys: `MusicBasePath`, `leftDivider`, `rightDivider`

**Data Files** (in music base directory):
- `database.xml` — tandas and playlists (XStream format); also contains tracks in legacy format
- `tracks.xml` — tracks in new XML format (used when `NEWTRACKDATABASE = true`)
- `database-YYYY-MM-DD_HH.mm.bak` — timestamped backups

## Dependencies

- **XStream** — Object-to-XML serialization (`AnyTypePermission.ANY` is set; all classes are deserializable)
- **Apache Commons Text** — Levenshtein distance for fuzzy search
- **TT2.SoundUtils** — External package (source not in this project); provides audio file validation
