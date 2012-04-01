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
    class Instrument extends Backbone.Model
      serverType: 'instrument'

    class Instruments extends Backbone.Collection
      model: Instrument

# ## Experiments
    class Experiment extends Common.Model
      serverType: 'experiment'
      embedded:
        instruments: ['references', Instruments]
        treatment: ['reference', Treatment]

    class Experiments extends Common.Collection
      model: Experiment

# ## My Trials
    class Trial extends Common.Model
      serverType: 'trial'
      embedded:
        experiment: ['reference', Experiment]

    class Trials extends Common.Collection
      model: Trial

# ## Trackers

    class Tracker extends Common.Model
      serverType: 'tracker'
      references:
        user: ['reference', User]
        instrument: ['reference', Instrument]

    class Trackers extends Common.Collection
      model: Tracker

# ## User Object
#
# - Always initialized by the server
# - Singleton model, so use uppercase instance convention

    class User extends Common.Model
      serverType: 'user'
      embedded:
        trials: ['submodel', Common.Trials]
        trackers: ['submodel', Common.Trackers]
        journals: ['submodel', Common.JournalEntries]

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



    # Return Core Models
    Treatment: Treatment
    Treatments: Treatments
    Instrument: Instrument
    Instruments: Instruments
    Experiment: Experiment
    Experiments: Experiments
    Trial: Trial
    Trials: Trials
    Tracker: Tracker
    Trackers: Trackers
    Suggestion: Suggestion
    Suggestions: Suggestions
    User: User
    Users: Users

