var World = {


    createModelAtLocation: function createModelAtLocationFn(modelData) {

            var location = new AR.GeoLocation(parseFloat(modelData[0].latitude), parseFloat(modelData[0].longitude));


        var modelCoin = new AR.Model("assets/mCoin.wt3", {
            onLoaded: this.worldLoaded,
            onError: World.onError,

            scale: {
                x: 1,
                y: 1,
                z: 1
            },
            rotate: {
                x: 0,
                y: -80,
                z: 90
              },

              onClick : function() {
//                  World.geoObject.destroy();
                    AR.context.destroyAll();
                }
        });


        var rotateAnimation = new AR.PropertyAnimation(modelCoin, "rotate.x", 0, 360, 10000);


        var indicatorImage = new AR.ImageResource("assets/indi.png", {
            onError: World.onError
        });

        var indicatorDrawable = new AR.ImageDrawable(indicatorImage, 0.1, {
            verticalAnchor: AR.CONST.VERTICAL_ANCHOR.TOP
        });

        this.geoObject = new AR.GeoObject( location , {
            drawables: {
                cam: [modelCoin],
                indicator: [indicatorDrawable]
            }
        });

        rotateAnimation.start(-1);

    },

    worldLoaded: function worldLoadedFn() {
       document.getElementById("loadingMessage").style.display = "none";
    }
};
