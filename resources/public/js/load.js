// Author: Ian Eslick
// Filename: load.js

// Require.js configuration file to load scripts

//				jquery: '//ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min',

require.config(
    {
		waitSeconds: 15,
		paths: {use: 'libs/require/use.min',
				jquery: 'libs/jquery/jquery-1.7.min',
				jqueryTimeAgo: 'libs/jquery/jquery.timeago',
				jQueryUI: 'libs/jquery/jquery-ui-1.8.18.custom.min',
				jQuerySparkline: 'libs/jquery/jquery.sparkline.min',
				jQuerySuggest: 'libs/jquery/jquery.autoSuggest.packed',
				jQueryDatePicker: 'libs/jquery/daterangepicker.jQuery',
				Handlebars: 'libs/misc/handlebars.1.0.0.beta.3',
				Underscore: 'libs/underscore/underscore-131',
				Bootstrap: 'libs/bootstrap/bootstrap.min',
				Backbone: 'libs/backbone/backbone',
				BackboneRelational: 'libs/backbone/backbone-relational',
				BackboneForms: 'libs/backbone/backbone-forms',
				BackboneFormsBS: 'libs/backbone/backbone-forms-bootstrap',
				BackboneFormsEditors: 'libs/backbone/jquery-ui-editors',
				D3: "libs/d3/d3", // d3.min
				D3time: "libs/d3/d3.time.min",
				QIchart: 'libs/qi-chart',
				Common: 'views/common',
                Moment: 'libs/misc/moment.min',
			   },
		use: { "Underscore": { attach: "_" },
			   "Handlebars": { attach: "Handlebars" },
			   "jqueryTimeAgo": { deps: ["jquery"] },
			   "jQueryUI": { deps: ["jquery"] },
			   "jQuerySuggest": { deps: ["use!jQueryUI"] },
			   "jQuerySparkline": { deps: ["use!jQueryUI"] },
               "jQueryDatePicker": { deps: ["use!jQueryUI", "libs/jquery/date"] },
			   "Bootstrap": { deps: ["jquery"] },
			   "Backbone": { deps: ["jquery", "use!Underscore"],
							 attach: "Backbone"},
			   "BackboneRelational": { deps: ["use!Backbone"] },
			   "BackboneForms": { deps: ["use!Backbone"] },
			   "BackboneFormsBS": { deps: ["use!BackboneForms", "use!Bootstrap"] },
			   "BackboneFormsEditors": { deps: ["use!BackboneForms", "use!jQueryUI"] },
			   "D3": { deps: ["jquery"],
					   attach: "d3"
					 },
			   "D3time": { deps: ["use!D3"] },
               "Moment": { deps: ["libs/misc/jstz.min"], attach: 'moment' }
			 },
		deps: ['views/common'],
		callback: function () {
			console.log('loaded common libs');
		}
	})

