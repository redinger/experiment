# Core Models for PersonalExperiments.org
# -------------------------------------------

# This modules defines all of the Personal Experiment
# domain models and appropriate convenience methods.
# Returns a map of Model constructors

define ['jquery', 'use!Backbone', 'models/infra', 'models/user'],
  ($, Backbone, Infra, UserSub) ->

# ## Comments
    class Comment extends Infra.Model
      serverType: 'comment'

    class Comments extends Infra.Collection
      model: Comment

# ## Treatments
    class Treatment extends Infra.Model
      @implements Infra.Taggable
      serverType: 'treatment'
      defaults:
        'tags': []
        'comments': []

      name: ->
        @get('name')

      cloneModel: ->
        clone = @clone()
        _.map ['id', 'help', 'comments', 'votes', 'warnings', 'tags'], (slot) ->
          clone.unset slot
        clone.set('name', clone.get('name') + ' (clone)')
        clone

      schema:
        name:
          type: 'Text'
          title: 'Title'
          editorClass: 'input-xlarge'
        description:
          type: 'TextArea'
          editorClass: 'input-xxlarge'
          editorAttrs:
            rows: '8'
        reminder:
          type: 'Text'
          title: 'Text for Reminders'
          editorClass: 'input-xxlarge'
        dynamics:
          type: 'Object'
          title: 'Treatment Impact'
          subSchema:
            onset:
              type: 'Number'
              title: 'Onset period'
              help: 'How many days from the start of the treatment until you see an effect?'
            washout:
              type: 'Number'
              title: 'Washout period'
              help: 'How many days after you stop treatment before you return to "normal"?'

    class Treatments extends Infra.Collection
      model: Treatment

# ## Instruments
    class Instrument extends Infra.Model
      @implements Infra.Taggable
      serverType: 'instrument'

      name: ->
        @get('variable')

      title: ->
        @get('variable') + " -- " + @get('service')

      track: (schedule) ->
        if schedule?
          schedule.event = @get('event') if @get('event')?
          schedule.event.type = 'event'
          schedule.event.wait = true
          console.log schedule

        tracker = theUser.trackers.create
          type: "tracker"
          user: theUser.asReference()
          instrument: @asReference()
          schedule: schedule
        @set 'tracked', true

      untrack: ->
        tracker = theUser.trackers.find (tracker) ->
            tracker.instrument.id is @id
        , @
        if tracker?
          tracker.destroy()
          @set('tracked', false)
        else
          alert 'Unable to untrack this tracker - contact the admin'

      schema:
        variable:
          type: 'Text'
          title: 'Variable Name'
          editorClass: 'input-large'
        description:
          type: 'TextArea'
          editorClass: 'input-xxlarge'
          editorAttrs:
            rows: '8'
        service:
          type: 'Text'
          title: 'Service Name'
          editorClass: 'input-large'
        event:
          type: 'Object'
          title: 'Manual Event Spec'
          subSchema:
            etype:
              type: 'Text'
            message:
              type: 'Text'
              editorClass: 'input-xxlarge'
            "sms-value-type":
              type: 'Select'
              title: 'SMS Value Type'
              options: ["int", "float", "string"]
            "sms-prefix":
              type: 'Text'
              title: 'SMS Response Prefix'

    class Instruments extends Infra.Collection
      model: Instrument

# ## Experiments
    class Experiment extends Infra.Model
      @implements Infra.Taggable
      serverType: 'experiment'
      embedded:
        treatment: ['reference', 'Treatment']
        outcome: ['references', 'Instruments']
        covariates: ['references', 'Instruments']

      name: ->
        @treatment.get('name') if @treatment?

    class Experiments extends Infra.Collection
      model: Experiment

# ## My Trials
    class Trial extends Infra.Model
      serverType: 'trial'
      embedded:
        experiment: ['reference', 'Experiment']
        user: ['reference', 'User']

      schema:
        start:
          type: 'Date'
          title: 'Start Date'
        reminders:
          type: 'Checkboxes'
          title: 'Enable Reminders?'
          options: ["SMS"] # "Email"]

      setDefaults: (model) ->
        @set('user', theUser)
        @set('start', Date())
        @set('reminders', ["SMS"])
        @set('status', "active")
        @

    class Trials extends Infra.Collection
      model: Trial

# ## Schedule
    class Schedule extends Infra.Model
      serverType: 'schedule'

# ## Event
    class Event extends Infra.Model
      serverType: 'event'
      embedded:
        instrument: ['reference', 'Instrument']
        user: ['reference', 'User']

# ## Trackers

    class Tracker extends Infra.Model
      serverType: 'tracker'
      embedded:
        user: ['reference', 'User']
        instrument: ['reference', 'Instrument']
        schedule: ['submodel', 'schedule']

    class Trackers extends Infra.Collection
      model: Tracker

# ## Journals
    class Journal extends Infra.Model
      serverType: 'journal'
      embedded:
        user: ['reference', 'User']

    class JournalEntries extends Infra.Collection
      model: Journal

# ## Service submodel containers
    class Service extends Infra.Model
      serverType: 'service'

    class Services extends Infra.Collection
      model: Service

# ## User Object
#
# - Always initialized by the server
# - Singleton model, so use uppercase instance convention

    class User extends Infra.Model
      serverType: 'user'
      embedded:
        trials: ['submodels', 'Trials']
        trackers: ['submodels', 'Trackers']
        preferences: ['submodel', 'userprefs']
        services: ['submodels', 'Services']
        journals: ['submodels', 'Journals']

      username: -> @get('username')
      adminp: -> 'admin' in @get('permissions')

    class Users extends Infra.Collection
      model: User

# ## Helper objects

    # Used to store data for search suggestions
    class Suggestion extends Backbone.Model

    class Suggestions extends Backbone.Collection
      model: Suggestion


# ## Register types and return direct references to constructors
    models =
      Treatment: Treatment
      Instrument: Instrument
      Experiment: Experiment
      Trial: Trial
      Schedule: Schedule
      Event: Event
      Tracker: Tracker
      Suggestion: Suggestion
      UserPrefs: UserSub.UserPrefs
      User: User
      Journal: Journal
      Service: Service

    collections =
      Treatments: Treatments
      Instruments: Instruments
      Experiments: Experiments
      Journals: JournalEntries
      Trials: Trials
      Trackers: Trackers
      Suggestions: Suggestions
      Services: Services
      Users: Users

    cacheTypes = (themap) ->
       newmap = {}
       _.each themap, (constructor, type) ->
             tag = constructor.prototype.serverType
             if tag
               newmap[tag] = constructor
             else
               newmap[type] = constructor
       newmap

    # Register our core types with the reference cache
    Backbone.ReferenceCache.registerTypes cacheTypes(models)
    Backbone.ReferenceCache.registerTypes collections

# ## Bootstrap data
    # Place an array of model attributes into a non-javascript script tag
    # with the bootstrap-models ID, and the session user attributes as an
    # object literal in bootstrap-user

    # A canonical object for the session user
    theUser = new User()

    # Bootstrap models into the reference cache
    Backbone.ReferenceCache.lazy = true
    Backbone.ReferenceCache.importFromID '#bootstrap-user', theUser
    Backbone.ReferenceCache.importFromID '#bootstrap-models'
    Backbone.ReferenceCache.lazy = false
    Backbone.ReferenceCache.loadAll()

    # Return Core Models and canonical instances
    _.extend {},models,collections,
        theUser: theUser
        cacheTypes: cacheTypes

