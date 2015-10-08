var app = angular.module('labels', ['ngResource'], function($locationProvider) {
	$locationProvider.html5Mode(true);
});

function LabelsController($scope, $resource, $location, $routeParams) {
	var NameTagsResource = $resource('../gui/nametags/');

	var chunk = function(list, chunksize) {
		var chunked = [];
		var rest = list;
		while (rest.length > 0) {
			chunked.push(_.first(rest, chunksize));
			rest = _.rest(rest, chunksize);
		}
		return chunked;
	};

	NameTagsResource.query(function(ls) {
		$scope.labels = ls; //chunk(ls, 10);
	});
}
