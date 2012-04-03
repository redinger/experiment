# Core Models for PersonalExperiments.org
# -------------------------------------------

# This modules defines all of the Personal Experiment
# domain models and appropriate convenience methods.
# Returns a map of Model constructors

define ['use!Backbone', 'models/infra'],
  (Backbone, Common) ->


# ## Treatments
    class Treatment extends Common.Model
      serverType: 'treatment'
      defaults:
        'tags': []
        'comments': []

    class Treatments extends Common.Collection
      model: Treatment

# ## Instruments
    class Instrument extends Common.Model
      serverType: 'instrument'

    class Instruments extends Common.Collection
      model: Instrument

# ## Experiments
    class Experiment extends Common.Model
      serverType: 'experiment'
      embedded:
        treatment: ['reference', 'treatment']
        instruments: ['references', 'Instruments']

    class Experiments extends Common.Collection
      model: Experiment

# ## My Trials
    class Trial extends Common.Model
      serverType: 'trial'
      embedded:
        experiment: ['reference', 'experiment']

    class Trials extends Common.Collection
      model: Trial

# ## Trackers

    class Tracker extends Common.Model
      serverType: 'tracker'
      embedded:
        user: ['reference', 'user']
        instrument: ['reference', 'instrument']

    class Trackers extends Common.Collection
      model: Tracker

# ## Journals
    class Journal extends Common.Model
      serverType: 'journal'
      embedded:
        user: ['reference', 'user']

    class JournalEntries extends Common.Collection
      model: Journal

# ## User Object
#
# - Always initialized by the server
# - Singleton model, so use uppercase instance convention

    class User extends Common.Model
      serverType: 'user'
      embedded:
        trials: ['submodels', 'Trials']
        trackers: ['submodels', 'Trackers']
        journals: ['submodel', 'Journals']

      username: -> @get('username')
      adminp: -> 'admin' in @get('permissions')

    class Users extends Common.Collection
      model: User

# ## Non-Core Models
# ------------------------------

# ## Suggestions
# Used to store data for search suggestions
    class Suggestion extends Backbone.Model

    class Suggestions extends Backbone.Collection
      model: Suggestion

# Register types and return direct references to constructors

    models =
      Treatment: Treatment
      Instrument: Instrument
      Experiment: Experiment
      Trial: Trial
      Tracker: Tracker
      Suggestion: Suggestion
      User: User

    collections =
      Treatments: Treatments
      Instruments: Instruments
      Experiments: Experiments
      Trials: Trials
      Trackers: Trackers
      Suggestions: Suggestions
      Users: Users

    cacheTypes = (themap) ->
       newmap = {}
       _.each themap, (constructor, type) ->
             tag = constructor.prototype.serverType
             newmap[tag] = constructor
       newmap

    Backbone.ReferenceCache.registerTypes cacheTypes(models)
    Backbone.ReferenceCache.registerTypes collections

    # Return Core Models
    _.extend {},models,collections

