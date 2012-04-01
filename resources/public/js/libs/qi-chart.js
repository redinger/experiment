/****

TODO:

Datapoint Definitions
  - All points are structs
  - Types for points: data, missing, event(?)

Metadata
  - periods -> shading regions, annotated regions
  - events -> box & arrow annotations

Control Charts
  - UCL/LCL
  - Points above/below UCL/LCL
  - Runs of points above/below mean

*****/

(function () {
define( "QIcharts", ["jquery", "use!Underscore", "use!D3time", ], 
function ($, _, d3) { 

var QIchart = {
    renderTimeAxis: function (chart,xScale,cfg) {

	var x_fmt = xScale.tickFormat(8);

	/* Grouping */
	var ticks = chart.selectAll('.tick')
	    .data(xScale.ticks(8))
	    .enter().append('svg:g')
	    .attr('transform', function(d) { return "translate(" + xScale(d) + ", 0)"; })
	    .attr('class', cfg.defaults.timeaxis);
	    
	/* Tick Marks */
	ticks.append("svg:line")
	    .attr("x1", 0)
	    .attr("x2", 0)
	    .attr("y1", cfg.h-10)
  	    .attr("y2", cfg.h);

	/* Labels */
	ticks.append("svg:text")
	    .text(x_fmt)
	    .attr("text-anchor", "end")
	    .style("fill", "black")
	    .attr("transform", "translate(3 9) rotate(320 0 " + cfg.h + ")")
	    .attr("y", cfg.h);
    },

    renderValueAxis: function(chart,yScale,offset,cfg) {
	/* Axis Line */
	chart.append("line")
	    .style("stroke", "black")
	    .style("stroke-width", "2px")
	    .attr("x1", 0)
	    .attr("x2", 0)
	    .attr("y1", 0)
	    .attr("y2", cfg.h);

	/* Heading */
	

	var ticks = chart.selectAll('.tick')
	    .data(yScale.ticks(5))
	    .enter().append('svg:g')
	    .attr('transform', function(d) {
		return "translate(0, " + yScale(d) + ")";})
	    .attr('class', cfg.defaults.valueaxis || "vaxis")

	/* Tick Marks */
	ticks.append("svg.line")
	    .attr("x1", 0)
	    .attr("x2", 10)
	    .attr("y1", 0)
	    .attr("y2", 0);

	/* Labels */
	ticks.append("svg:text")
	    .attr("text-anchor", "end")
	    .attr("transform", "translate(-5 0)")
            .style("fill", "black")
            .style("stroke", "none")
	    .attr("x", 0)
	    .attr("y", 0)
	    .text(String);
	
    },

    mean: function(data) {
	return _.reduce(data, function (a,b) { return a + b; }) / _.size(data);
    },

    dates: function(pairs) { return _.toArray(_.map(pairs, function(p) { return p[0]; })) },
    values: function(pairs) { return _.toArray(_.map(pairs, function(p) { return p[1]; })) },

    xScale: function(data,cfg) {
	return d3.time.scale()
	    .domain([d3.min(data), d3.max(data)])
	    .range([20,cfg.w-20]);
    },
    
    yScale: function(data,cfg) {
        return d3.scale.linear()
            .domain([0, d3.max(data)])
            .range([cfg.h,25]);
    },

    renderSeries: function(chart,series,cfg,mean) {
	var ds = this.dates(series);
	var vs = this.values(series);

	var x = this.xScale(ds, cfg); /* _.pluck(series,"ts")); */
	var y = this.yScale(vs, cfg);

	chart.selectAll("circle")
	    .data(series)
	    .enter().append("circle")
	    .attr("class", cfg.defaults.glyph)
	    .attr("cx", function (d,i) { return x(d[0]); })
	    .attr("cy", function (d,i) { return y(d[1]); })
	    .attr("r", 3);

	chart.selectAll("path.line")
	    .data([series])
	    .enter().append("svg:path")
	    .attr("stroke-linejoin", "round")
	    .attr("d", d3.svg.line()
		  .x( function(d,i) { return x(d[0]); } )
		  .y( function(d,i) { return y(d[1]); } ));

	/* Mean line */
	if (mean) {
    	    var mean = this.mean(vs);
	    chart.append("line")
		.attr("stroke-width", "1px")
		.attr("stroke", "black")
	        .attr("fill", "black")
	        .attr("stroke-opacity", "0.5")
		.attr("x1", 0)
		.attr("y1", cfg.h-y(mean))
		.attr("x2", cfg.w)
		.attr("y2", cfg.h-y(mean));
	};
    },

    renderRegionHighlight: function (chart, xScale, region, cfg) {
	var x1 = xScale(region.start);
	var x2 = xScale(region.end);
	var width = x2 - x1;

	chart.append('rect')
	    .style("fill", "red")
	    .style("fill-opacity", 0.25)
	    .attr("x", x1)
	    .attr("y", 0)
	    .attr("height", cfg.h)
	    .attr("width", width);
	
	if (region.label) {
	    chart.append('text')
	        .attr("x", x1 + (x2 - x1) / 2)
	        .attr("y", 20)
	        .attr("text-anchor", "middle")
	        .style("fill", "black")
	        .style("stroke", "none")
	        .text(region.label);
	}

    },
  
    boundingBox: function (chart, cfg) {
	chart.append('rect')
	    .style("stroke", "grey")
	    .style("fill", "none")
	    .attr("x", 0)
	    .attr("y", 0)
	    .attr("width", cfg.w)
	    .attr("height", cfg.h);
    },

    runChart: function (cid,data,cfg) {
	var chart = d3.selectAll(cid).append("svg")
  	    .attr("class", cfg.defaults.chart)
	    .attr("width", cfg.w + cfg.margin*2)
	    .attr("height", cfg.h + cfg.margin*3)
	    .append("g")
	    .attr("transform", "translate(" + cfg.margin * 2 + "," + cfg.margin + ")");

	var ds = this.dates(data);
	var vs = this.values(data);

	var x = this.xScale(ds, cfg);
	var y = this.yScale(vs, cfg);

	this.boundingBox(chart, cfg);
        this.renderTimeAxis(chart,x,cfg);
	this.renderValueAxis(chart,y,0,cfg);
	this.renderSeries(chart,data,cfg,true);

	// Temporary demo data
	this.renderRegionHighlight(chart, x,
	   {"start": 1329091200000, 
	    "end": 1329436800000, 
	    "label": "Feeling sick"}, cfg);
	this.renderRegionHighlight(chart, x,
	   {"start": 1330473600000,
	    "end": 1330646400000,
	    "label": "On Flagyl"}, cfg);
    },

    controlChart: function (cid,d,cfg) {
	alert( 'not implemented' );
    }
};
    return QIchart;
});

})();

// Example:

// var data = [[1328313600000, 2.21], [1328400000000, 1.19], [1328486400000, 0.59], [1328572800000, 6.8], [1328659200000, 4.74], [1328745600000, 12.72], [1328832000000, 9.49], [1328918400000, 3.94], [1329004800000, 4.95], [1329091200000, 4.79], [1329177600000, 7.78], [1329264000000, 5.62], [1329350400000, 6.76], [1329436800000, 7.28], [1329523200000, 3.03], [1329609600000, 2.83], [1329696000000, 8.28], [1329782400000, 8.54], [1329868800000, 2.73], [1330041600000, 6.28], [1330128000000, 0.62], [1330300800000, 10.39], [1330387200000, 12.08], [1330473600000, 12.38], [1330560000000, 9.17], [1330646400000, 3.88], [1330732800000, 0.31]];

// var width = 850;
// var height = 180;
// var margin = 20;

// var cfg = {w: width,
// 	   h: height,
// 	   defaults: { glyph: "glyph",
//                       chart: "chart2",
// 		       timeaxis: "rule"},
// 	   margin: margin,
// 	  };


// $(document).ready(function () {
//   window.QIchart.runChart("#qichart",data,cfg);
// });

