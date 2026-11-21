# Match day checklist

Print this or keep it open on the laptop.

## The week before

- ☐ Install the latest APK on all six tablets
      ([Build and install](../maintain/build-and-install.md)).
- ☐ Charge everything: tablets, laptop, battery packs.
- ☐ Pair every scouting tablet with the master in Android's Bluetooth
      settings.
- ☐ On the laptop, in **Admin**:
    - ☐ Team number, season, **current event**.
    - ☐ Scouter roster up to date.
    - ☐ **Get team's events from TBA** and **Download schedule** for the
          event (schedules usually appear a day or two before quals).
    - ☐ Assign each tablet its station.
- ☐ Connect to the master (LoRa or TCP) and **Send to tablets**.
- ☐ On each scouting tablet, **Bluetooth Sync → Connect & Sync** once, so
      it gets the roster, schedule and station.
- ☐ Check on one tablet: **View Events** shows the event and its matches,
      and the scout list has the right names.

```{admonition} If the schedule isn't on TBA yet
:class: tip
Do the steps above anyway, then download the schedule and push again once
it's published. Tablets can also use **Get Current Matches** if they have
internet.
```

## At the event

- ☐ Master tablet: **Bluetooth Sync → Start Hosting**. Leave it hosting.
- ☐ Each scout: **Bluetooth Sync → Connect & Sync** at the start of the
      day. While connected, new matches sync automatically.
- ☐ Laptop: connect to the master and tick **Sync automatically**.
- ☐ Every few matches, look at the **Scouting coverage** dashboard for gaps.
- ☐ If a tablet drops off Bluetooth: reconnect it. Nothing is lost; it
      catches up on reconnect.
- ☐ If Bluetooth won't work at all: scouts show the **Submit** QR code;
      master uses **Sync With Workers** to scan.

## Alliance selection

- ☐ Sync everything first (scouting tablets → master → laptop).
- ☐ Use **Alliance selection** on the master (or any synced tablet). Keep
      the pick list up to date as teams get picked.

## After the event

- ☐ Final sync to the laptop.
- ☐ On the laptop: **Data → Export CSV…** for the event, and back up
      `scouting.db` (copy it somewhere safe).
- ☐ Optionally copy the tablets' raw files into the team's `ScoutingData`
      archive (`<year>_Data/<event>/Raw_JSON`).
