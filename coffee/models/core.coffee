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

    class Treatments extends Infra.Collection
      model: Treatment

# ## Instruments
    class Instrument extends Infra.Model
      @implements Infra.Taggable
      serverType: 'instrument'

      name: ->
        @get('variable')

      track: (schedule) ->
        tracker = theUser.trackers.create
          type: "tracker"
          user: theUser
          instrument: @model
          schedule: schedule
        tracker.save()
        @

      untrack: ->
        tracker = _.find theUser.trackers, (tracker) =>
            tracker.instrument.id is @model.id
        tracker.destroy()
        @

    class Instruments extends Infra.Collection
      model: Instrument

# ## Experiments
    class Experiment extends Infra.Model
      @implements Infra.Taggable
      serverType: 'experiment'
      embedded:
        treatment: ['reference', 'Treatment']
        instruments: ['references', 'Instruments']

      name: ->
        @treatment.get('name') if @treatment?

    class Experiments extends Infra.Collection
      model: Experiment

# ## My Trials
    class Trial extends Infra.Model
      serverType: 'trial'
      embedded:
        experiment: ['reference', 'Experiment']

    class Trials extends Infra.Collection
      model: Trial

# ## Schedule
    class Schedule extends Infra.Model
      serverType: 'schedule'

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

