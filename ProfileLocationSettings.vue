<template>
  <div class="settingsContainer">
    <UserSettingsMenu/>
    <div class="settingsContentContainer">
      <h1 class="settingsTitle">Edit Profile Location</h1>
      <hr/>
      <div id="userSettingsMap"></div>
      <hr/>
      <v-container>
        <v-row justify="center" align="center">
          <v-icon large>mdi-arrow-up-thick</v-icon>
        </v-row>
        <v-row justify="center" align="center"><h4 style="color:var(--v-primaryText-base);">Select on map or type in the box</h4></v-row>
        <v-row justify="center" align="center">
          <v-icon large>mdi-arrow-down-thick</v-icon>
        </v-row>

      </v-container>
      <div class="locationFieldDiv">
        <v-text-field id="locationInput" v-model="address" class="locationInput" label="Address" outlined dense></v-text-field>
      </div>
      <button class="genericConfirmButton updatePasswordButton" style="margin-top: 10px" v-on:click="updateProfile()" type="submit">Save
        Location
      </button>
    </div>
    <div class="floatClear"></div>
    <v-snackbar
        v-model="snackbar"
        :color="snackbarColour"
    >
      {{ snackbarText }}
      <v-btn
          @click="snackbar = false"
          color="white"
          text
      >
        Close
      </v-btn>
    </v-snackbar>
  </div>
</template>

<script>
  import UserSettingsMenu from "./ProfileSettingsMenu";
  import {mapActions, mapGetters, mapState} from "vuex";
  import mapStyles from "../../../util/mapStyles";

  export default {
    name: "ProfileLocationSettings",
    components: {
      UserSettingsMenu
    },
    props: ['darkModeGlobal'],
    data: function () {
      return {
        searchedUser: {},
        snackbar: false,
        snackbarText: null,
        snackbarColour: '',
        location: {
          street_address: null,
          suburb: null,
          postcode: null,
          city: null,
          state: null,
          country: null,
          latitude: null,
          longitude: null,
        },
        mapMarker: null,
        map: null,
        autocomplete: null,
        address: ""
      };
    },
    computed: {
      ...mapState(["user"]),
      ...mapGetters(["user"]),
    },
    /**
     * On start-up, adds a listener to locationInput such that a query is made to Photon when the user stops typing
     * after 1 second. Calls a support function to add a summary key for each of the location objects. Locations with
     * duplicate summaries are removed.
     */
    async mounted() {
      this.loadSearchedUser();
      this.loadMap();
    },
    methods: {
      ...mapActions(["logout", "updateUserProfile", "getUserById", "editProfile", "getDataFromUrl", "editUserLocation"]),

      /**
       * Creates an event handler to check if the theme has changed
       * @param map
       */
      setThemeCheckEvent(map) {
        let outer = this;
        window.google.maps.event.addDomListener(window, 'click', function() {
          map.setOptions({
            styles: mapStyles[outer.darkModeGlobal ? "dark" : "light"],
            backgroundColor: '#696969'
          });
        });
      },

      /**
       * Loads the map onto the page and centres on the users home city.
       * Adds a marker on the city's centre.
       */
      loadMap() {
        if (!window.google) {
          return;
        }
        this.geocoder = new window.google.maps.Geocoder();

        let thisInner = this;

        this.map = new window.google.maps.Map(document.getElementById("userSettingsMap"), {
          center: position,
          zoom: 10,
          backgroundColor: '#696969'
        });

        let position = null;
        let zoomLevel = 2;

        if (this.searchedUser.location) {
          zoomLevel = 10;
          position = new window.google.maps.LatLng(this.searchedUser.location.latitude, this.searchedUser.location.longitude);
        } else {
          position = new window.google.maps.LatLng(0, 0);
        }

        thisInner.map = new window.google.maps.Map(document.getElementById("userSettingsMap"), {
          center: position,
          zoom: zoomLevel,
          streetViewControl: false,
          styles: mapStyles[this.darkModeGlobal ? "dark" : "light"],
          fullscreenControl: false,
          rotateControl: false,
          mapTypeControl: false,
          backgroundColor: '#696969'
        });

        if (this.searchedUser.location) {
          const latLng = new window.google.maps.LatLng(this.searchedUser.location.latitude, this.searchedUser.location.longitude);
          thisInner.mapMarker = new window.google.maps.Marker({
            position: latLng,
            map: thisInner.map
          });
        }

        thisInner.map.addListener('click', function (e) {
          if (thisInner.mapMarker != null) {
            thisInner.mapMarker.setMap(null);
          }
          thisInner.mapMarker = new window.google.maps.Marker({
            position: e.latLng,
            map: thisInner.map,
            backgroundColor: '#696969'
          });
          thisInner.map.panTo(e.latLng);
          thisInner.getLocationFromLatLng(e.latLng);
        });

        this.autocomplete = new window.google.maps.places.Autocomplete(
            document.getElementById("locationInput"),
            {
              types: ["address"],
            }
        );

        this.autocomplete.addListener("place_changed", this.fillInAddress);
        this.setThemeCheckEvent(thisInner.map);
      },


      /**
       * Fills in address field and location object from address autocomplete
       */
      async fillInAddress() {
        let thisInner = this;
        const place = this.autocomplete.getPlace();
        this.location = await this.extractLocationData(place);
        this.updateAddressString(this.location);
        if (this.mapMarker != null) {
          this.mapMarker.setMap(null);
        }
        this.mapMarker = new window.google.maps.Marker({
          position: place["geometry"]["location"],
          map: thisInner.map,
          backgroundColor: '#696969'
        });
        thisInner.map.panTo(place["geometry"]["location"]);
      },

      /**
       * Extractor function that parses the google maps response and returns a location object.
       */
      async extractLocationData(place) {
        let newLocation = {street_address:"",suburb:"",postcode:"",city:"",state:"",country:"",latitude:"",longitude:""};
        let addressComponents = place["address_components"];
        newLocation["latitude"] = place["geometry"]["location"].lat();
        newLocation["longitude"] = place["geometry"]["location"].lng();

        for (let i = 0; i < addressComponents.length; i++) {
          let content = addressComponents[i]["long_name"];
          if(addressComponents[i]["types"].includes("subpremise")){newLocation.street_address += content+"/";}
          if(addressComponents[i]["types"].includes("street_number")){newLocation.street_address += content;}
          if(addressComponents[i]["types"].includes("route")){newLocation.street_address += " "+content}
          if(addressComponents[i]["types"].includes("sublocality")){newLocation.suburb = content}
          if(addressComponents[i]["types"].includes("locality")){newLocation.city = content}
          if(addressComponents[i]["types"].includes("administrative_area_level_1")){newLocation.state = content}
          if(addressComponents[i]["types"].includes("country")){newLocation.country = content}
          if(addressComponents[i]["types"].includes("postal_code")){newLocation.postcode = content}
        }
        newLocation.street_address = newLocation.street_address.trim();

        return newLocation;
      },

      /**
       * Calls google maps api with lat lng and updates the location object
       */
      getLocationFromLatLng(latlng) {
        let thisInner = this;
        this.geocoder.geocode({'location': latlng}, async function (results, status) {
          if (status === 'OK') {
            if(results.length === 0){
              this.snackbarText = "Invalid Location";
              this.snackbarColour = "error";
              this.snackbar = true;
            }else{
              thisInner.location = await thisInner.extractLocationData(results[0]);
              thisInner.updateAddressString(thisInner.location);
            }
          } else {
            this.snackbarText = status;
            this.snackbarColour = "error";
            this.snackbar = true;
          }
        });
      },

      /**
       * Updates the address string from the locationObject parameter. Used when the location is changed by clicking
       * on the map, and when the map is first loaded with the initial location.
       */
      updateAddressString(locationObject) {
        if (locationObject) {
          this.address = "";
          if (locationObject.street_address !== "") {
            this.address += locationObject.street_address
          }
          if (locationObject.suburb !== "") {
            if (this.address !== "") {
              this.address += ", "
            }
            this.address += locationObject.suburb;
          }
          if (locationObject.city !== "") {
            if (this.address !== "") {
              this.address += ", "
            }
            this.address += locationObject.city;
          }
          if (locationObject.state !== "") {
            if (this.address !== "") {
              this.address += ", "
            }
            this.address += locationObject.state;
          }
          if (locationObject.country !== "") {
            if (this.address !== "") {
              this.address += ", "
            }
            this.address += locationObject.country;
          }
        }
      },


      /**
       Sends a request to the server side to update the searchedUser's profile info. Displays error messages if the update
       was unsuccessful.
       */
      updateProfile() {
        this.searchedUser.location = this.location;
        this.editProfile(this.searchedUser)
            .then(
                response => {
                  // check if you are a normal user updating profile, then show the change on frontend
                  if (this.user.profile_id === parseInt(this.$route.params.profileId)){
                    this.updateUserProfile(this.searchedUser);
                  }
                  this.snackbarText = response.data;
                  this.snackbarColour = "success";
                  this.snackbar = true;
                },
                error => {
                  this.snackbarText = error.response.data.Errors;
                  this.snackbarColour = "error";
                  this.snackbar = true;
                }
            );
      },

      /**
       * Uses user id from url to request user data.
       */
      async loadSearchedUser() {
        if (
            this.$route.params.profileId == null ||
            this.$route.params.profileId === ""
        ) {
          this.$router.push("/settings/profile/" + this.user.profile_id);
          this.searchedUser = this.user;
        } else {
          let tempUserData = await this.getUserById(this.$route.params.profileId);
          if (tempUserData === "Invalid permissions") {
            this.$router.push("/settings/profile/" + this.user.profile_id);
            this.searchedUser = this.user;
          } else {
            this.searchedUser = tempUserData;
          }
        }
        if(this.searchedUser.location != null){
          this.location = this.searchedUser.location;
        }
        this.updateAddressString(this.searchedUser.location);
        this.loadMap();
      }
    },
  }
</script>

<style scoped>
  @import "../../../../public/styles/pages/profileSettingsStyle.css";
</style>