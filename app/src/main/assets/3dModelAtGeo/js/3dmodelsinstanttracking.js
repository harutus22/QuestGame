var currentModel = null;
var indicator = null;
var currentModelKey = null;

var World = {


    createOverlaysCalled: false,


    createModel: function createModelFn() {
        if (World.createOverlaysCalled) {
            return;
        }

        World.createOverlaysCalled = true;


        this.tracker = new AR.InstantTracker({

            deviceHeight: 1.0,
            onError: World.onError,
            onChangeStateError: World.onError
        });

        this.instantTrackable = new AR.InstantTrackable(this.tracker, {

            onTrackingStarted: function onTrackingStartedFn() {
                /* Do something when tracking is started (recognized). */
            },
            onTrackingStopped: function onTrackingStoppedFn() {
                /* Do something when tracking is stopped (lost). */
            },
            onTrackingPlaneClick: function onTrackingPlaneClickFn() {
                /*
                    xPos and yPos are the intersection coordinates of the click ray and the instant tracking plane.
                    They can be applied to the transform component directly.
                */
            },
            onError: World.onError
        });



        var location1 = new AR.GeoLocation(40.372744, 44.950028);


        this.geoObject = new AR.GeoObject(location1, {
                radar: this.radardrawables
                });


    },

    changeTrackerState: function changeTrackerStateFn() {

        if (this.tracker.state === AR.InstantTrackerState.INITIALIZING) {
            this.tracker.state = AR.InstantTrackerState.TRACKING;
        } else {
            this.tracker.state = AR.InstantTrackerState.INITIALIZING;
        }
    },



    addModel: function addModelFn(modelData) {

     currentModelKey = modelData[0].key;
     var location = new AR.GeoLocation(parseFloat(modelData[0].latitude), parseFloat(modelData[0].longitude));

            var model = new AR.Model("assets/mCoin.wt3", {
            onLoaded: this.worldLoaded,
            onError: World.onError,

                scale: {
                    x: 1,
                    y: 1,
                    z: 1
                },
                rotate: {
                    x: 90,
                    y: -80,
                    z: 90
                },

                onClick : function() {
                                   World.reportToJava();
                                   World.resetModels();
                                }

            });


            if(this.tracker.state === AR.InstantTrackerState.TRACKING){
            var rotateAnimation = new AR.PropertyAnimation(model, "rotate.z", 0, 360, 10000);
                        rotateAnimation.start(-1);
            this.instantTrackable.drawables.addCamDrawable(model);
            } else{

            var indicatorImage = new AR.ImageResource("assets/indi.png", {
                                onError: World.onError
                            });

            var indicatorDrawable = new AR.ImageDrawable(indicatorImage, 0.1, {
                verticalAnchor: AR.CONST.VERTICAL_ANCHOR.TOP
            });

            indicator = indicatorDrawable;

            var rotateAnimation = new AR.PropertyAnimation(model, "rotate.x", 0, 360, 10000);
                        rotateAnimation.start(-1);
            this.geoObject.locations = location;
            this.geoObject.drawables.addCamDrawable(model);
            this.geoObject.drawables.addIndicatorDrawable(indicatorDrawable);
            }

            currentModel = model;

    },

    resetModels: function resetModelsFn() {
        if(this.tracker.state === AR.InstantTrackerState.TRACKING){
              this.instantTrackable.drawables.removeCamDrawable(currentModel);
              } else {
              this.geoObject.drawables.removeCamDrawable(currentModel);
              this.geoObject.drawables.removeIndicatorDrawable(indicator);
              }
              currentModel = null;
              indicator = null;
              currentModelKey = null;

          },

     reportToJava: function reportToJavaFn(){

                    var objectSelectedJSON = {
                        name: "objectClicked",
                        key: currentModelKey
                    };
                    AR.platform.sendJSONObject(objectSelectedJSON);
             },


    onError: function onErrorFn(error) {
        alert(error);
    },

    worldLoaded: function worldLoadedFn() {
           document.getElementById("loadingMessage").style.display = "none";
        }


};

World.createModel();