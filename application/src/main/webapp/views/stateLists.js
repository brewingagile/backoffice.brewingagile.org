function StateListsController($scope, $location, RegistrationsByState) {
	$scope.m = RegistrationsByState.get();

	$scope.getClass = function(path) {
		if ($location.path().substr(0, path.length) == path) {
			return "active";
		} else {
			return "";
		}
	};
}
