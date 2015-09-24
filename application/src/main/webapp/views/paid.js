function PaidController($scope, RegistrationsByState) {
	RegistrationsByState.get({}, function(d) {
		$scope.registrations = d.paid;
	});
}