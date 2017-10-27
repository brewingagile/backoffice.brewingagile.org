function AccountController($scope, $resource, $routeParams) {
	var R = $resource('gui/accounts/:account', {account: $routeParams.account});
	var Outvoice = $resource('gui/accounts/:account/invoices', {account: $routeParams.account});
	var Invoice = $resource('gui/accounts/:account/invoice', {account: $routeParams.account});
	var BillingInfo = $resource('gui/accounts/:account/billing', {account: $routeParams.account});
	$scope.account = R.get();
	$scope.outvoice = Outvoice.get();

	$scope.invoice = function() {
	    $scope.invoiceAlert = null;
	    Invoice.save({}, function(d) { $scope.invoiceAlert = d; });
	};

	$scope.saveBillingInfo = function() {
	    BillingInfo.save($scope.account, function(d) {
	        $scope.editBillingInfo = false;
	    });
	}
}
