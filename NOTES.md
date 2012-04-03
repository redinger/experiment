# The Experiment Project -- Design Notes

## Platform Architecture

Basic partitioning is that the server handles data processing and the
client handles visualization and editing of models, asking the server
to perform pre-processing of data resulting from client queries that
can be visualized on a front-end.

The rich-client app is a subsection of the site for which any URL from
the base "/app*" renders the app and client handles the rest of the
URL.  Basic support is provided for dispatching on the user-agent or a
cookie value for different web-based clients.

A mobile app client can use the APIs directly and bypass the /app
namespace.

A client app consists of:
   - HTML skeleton for site frame and place holder divs for app modules
   - Application javascript files
   - Model templates for dynamic rendering
   - Boostrap data for the specific sub-view being requested [This means the server needs to have a way of determining what data is being rendered and some duplication of URL parsing between server and client]

The server exposes various APIs to clients:
   - /api/root/...  - A backbone API to enable fetching and updating of models
   - /api/embed/...  - A submodel backbone extension API for embedded models
   - /api/suggest/... - A set of autoSuggest AJAX APIs for helping the UI
   - /api/chart/... - Renders chart content for various model / data types
   - /api/events/calendar - Renders a calendar of reminders or past events
   - /api/events/upcoming - Renders a calendar of reminders or past events

## Models

The server manages and provides core and function-specific APIs for a rich-client
model and some of the supplemental page logic.  


   - User
   - Embedded Objects for various objects
      - Journals
      - Comments

### Schematic layer 

- Experiment
  - Treatment
  - Instrument[]
  - Schedule (schema)

- Trial
  - Treatment
  - Trackers[]
  - Reminder
  - Schedule (realized)

### Operative Layer


## Tracking Workflow

A study has a schedule (one of a number of templates). A dispatch
function against the schedule type can transform a schedule into
reminders or serve as the backdrop for user-generated events.
   - Generates reminders
       - Reminders can be put on a calendar
   - User PROs are stored as time series in 'trackers'
       - Background API dumps generated events (dep on instrument)
       - Tracker objects record a series of data points from an instrument
       - Trackers can also be extracted to show recording events on a calendar
   - Trackers are processed to create canned reports or charts for a time period

     

# Releases

## v0.1 - Academy Health Prototype Release (November 15th, 2011)

## Tasks for v0.2 - Beta Release (April 10th, 2012)

Architectural design tasks

   - X Model abstraction on server
     - X Backbone-based abstraction layer (base classes, protocols) for the 'model model'
     - X Handle references
     - - Bootstrapping data into views (transmit entire DB for phase I)
     - X Nested object REST API support for Backbone
   - X Auto-complete for tags, model names, etc.
   - X Structured treatment descriptions using auto-complete
   - X Site navigation model using dispatch
   - X Proper support for dev/prod mode and versioning
   - X Break coffeescript into multiple independent UI modules
   - X Support proper logging through server and client side
   - X Google analytics integration
   - Error handling across server & client
   - Database and server backup
   - Sensible dump and reload database
   - ? Pallet distribution model
      - pallet, git, keys
      ~ Mongo installation, nginx as proxy and static files
      - TODO: Leinengin
      - TODO: Site upgrade scripts
   - NO IE popup on login

Feature tasks

   - X Study registration
   - X Editorial content
   - X Refactor Trial UI into components
   - X Tracker backend services
   - X Profile editing / forms
     - X Pretty forms CSS or JS
   - X Dashboard main page
     - X Outcome control chart
     - Illustrate missing data chart
     - Trial schedule and view
     - Date range selection for tracker page / trial view
     - Calendar click support
     - I want to click on tracking to change data
   - Explore Page
     - Style search object views
     - Start trial from experiment view
       - Handle schedule and reminders 
     - Object create / edit screens
   - Social components

## Tasks for v0.2 - Stability Release

Architecture design tasks

   - ~ Cache model templates in external files for release
   - ~ Release model for aggregated/versioned javascript files
   - Model abstraction on server
      - Handle synchronization conflicts for models
      - Define important properties like type checks, etc
   - Review / Improve error handling and validation
   - Dynamic loading, caching, and pre-rendering Templates, etc.
   - Server-side caching of rendered templates

Feature tasks

   - Full UX and UI review   

# Open Platform Issues
# ----------------------------------------------------------

## Longer term architectural issues

   - X Server-side dispatch to client 'views' based on user-agent.  E.g. iphone
     sends mobile client-app, search engine gets simple HTML view, web clients
     get web app and older browsers get 'install new browser' page.
   - Support search indexibility and SEO by rendering sub-views with
     proper links on the server side for search crawlers or
     accessibility.  The server-side of a given application will have
     to handle this.  (For example, we want nice SEO URLs for all
     browseable objects such as treatments, experiments, and public
     discussions.
     
## Wishlist / Notes

   - Site introduction similar to Coda's: http://www.panic.com/coda/
   - I like github's look at feel!  (as you can tell)



# Design Notes
# --------------------------------------------------------------

## Schedules and events

### Schedule

The calendar schedule for an event.

Weekdays, weekends, specific day, select days
TimeOfDay

Schedule Protocol
(events schedule interval)

### Events

Events have types w/ parameters that determine how the action plays out

### Actions

For now, baked into code.  

   - Sample Data: Elicit an SMS instrument value
   - Remind: the user of treatment period (start/stop, daily)
   - ? Report: study is finished, weekly progress?
