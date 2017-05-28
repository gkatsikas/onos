/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 ONOS GUI -- Metron Server Manager View Module
 */
(function () {
    'use strict';

    // injected references
    var $log, $scope, $location, ks, fs, cbs, ns;

    var coresNb = 16;
    var hasDeviceId;

    var labels = new Array(1);
    var data = new Array(coresNb);
    for (var i = 0; i < coresNb; i++) {
        data[i] = new Array(1);
        data[i][0] = 0;
    }

    angular.module('ovServercpu', ["chart.js"])
        .controller('OvServercpuCtrl',
        ['$log', '$scope', '$location', 'FnService', 'ChartBuilderService', 'NavService',

        function (_$log_, _$scope_, _$location_, _fs_, _cbs_, _ns_) {
            var params;
            $log = _$log_;
            $scope = _$scope_;
            $location = _$location_;
            fs = _fs_;
            cbs = _cbs_;
            ns = _ns_;

            params = $location.search();

            if (params.hasOwnProperty('devId')) {
                $scope.devId = params['devId'];
                hasDeviceId = true;
            } else {
                hasDeviceId = false;
            }

            cbs.buildChart({
                scope: $scope,
                tag: 'servercpu',
                query: params
            });

            $scope.$watch('chartData', function () {
                if (!fs.isEmptyObject($scope.chartData)) {
                    $scope.showLoader = false;
                    var length = $scope.chartData.length;
                    labels = new Array(length);
                    for (var i = 0; i < coresNb; i++) {
                        data[i] = new Array(length);
                    }

                    $scope.chartData.forEach(
                        function (cm, idx) {
                            data[0][idx]  = cm.cpu_0;
                            data[1][idx]  = cm.cpu_1;
                            data[2][idx]  = cm.cpu_2;
                            data[3][idx]  = cm.cpu_3;
                            data[4][idx]  = cm.cpu_4;
                            data[5][idx]  = cm.cpu_5;
                            data[6][idx]  = cm.cpu_6;
                            data[7][idx]  = cm.cpu_7;
                            data[8][idx]  = cm.cpu_8;
                            data[9][idx]  = cm.cpu_9;
                            data[10][idx] = cm.cpu_10;
                            data[11][idx] = cm.cpu_11;
                            data[12][idx] = cm.cpu_12;
                            data[13][idx] = cm.cpu_13;
                            data[14][idx] = cm.cpu_14;
                            data[15][idx] = cm.cpu_15;

                            labels[idx] = cm.label;
                        }
                    );
                }

                $scope.labels = labels;
                $scope.data = data;

                // Fixed y-axis range in [0, 100]
                $scope.options = {
                    scaleOverride: true,
                    scaleStartValue: 0,
                    scaleSteps: 10,
                    scaleStepWidth: 10,
                    scaleFontSize : 18
                };

                $scope.onClick = function (points, evt) {
                    var label = labels[points[0]._index];
                    if (label) {
                        ns.navTo('servercpu', { devId: label });
                        $log.log(label);
                    }
                };

                if (!fs.isEmptyObject($scope.annots)) {
                    $scope.deviceIds = JSON.parse($scope.annots.deviceIds);
                }

                $scope.onChange = function (deviceId) {
                    ns.navTo('servercpu', { devId: deviceId });
                };
            });

            $scope.series = [
                'CPU 0',  'CPU 1',  'CPU 2',  'CPU 3',
                'CPU 4',  'CPU 5',  'CPU 6',  'CPU 7',
                'CPU 8',  'CPU 9',  'CPU 10', 'CPU 11',
                'CPU 12', 'CPU 13', 'CPU 14', 'CPU 15',
            ];
            $scope.labels = labels;
            $scope.data = data;

            Chart.defaults.global.colours = $scope.chartColors;

            $scope.showLoader = true;

            $log.log('OvServercpuCtrl has been created');
        }]);

}());
