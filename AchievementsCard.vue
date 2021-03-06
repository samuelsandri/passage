<template>
  <v-card id="achievementCard" class="activityPageAchievementCard" :loading="loadingResults || loading" :disabled="loading">
    <div class="resultsDiv">
      <h2 id="achievementCardTitle" class="activityCardTitle">Achievements</h2>
      <h5 class="loadingInfoHeader" v-if="achievements.length === 0 && !loadingResults">No Achievements Set</h5>
      <h5 class="loadingInfoHeader" v-if="loading || loadingResults">Loading Achievements...</h5>
      <v-card class="achievementCard" v-for="achievement in achievements" v-bind:key="achievement.id" outlined >
        <v-row no-gutters style="padding: 10px 15px 9px;">
          <h3 style="padding-right: 5px;color: var(--v-primaryText-base);">{{achievement.name}}</h3>
          <v-tooltip bottom max-width="500px">
            <template v-slot:activator="{ on }">
              <v-icon v-on="on" style="font-size: 20px;">mdi-help-circle-outline</v-icon>
            </template>
            <span style="color: white;">{{achievement.description}}</span>
          </v-tooltip>
        </v-row>
        <v-divider v-if="results[achievement.id] !== undefined"></v-divider>
        <v-container v-if="results[achievement.id] !== 0" style="max-height: 200px;overflow-y: auto;">
          <h4 style="padding:5px 0;color: var(--v-primaryText-base);" v-for="(results, index) in results[achievement.id]" v-bind:key="index">Result {{index+1}}: {{results.value}}</h4>
        </v-container>
        <v-divider></v-divider>
        <v-row no-gutters style="padding: 10px 10px 6px;">
          <v-menu v-if="achievement.resultType === 'TIME'"
                  ref="menu"
                  v-model="clockBind[achievement.id]"
                  :close-on-content-click="false"
                  :nudge-right="40"
                  transition="scale-transition"
                  offset-y
                  max-width="290px"
                  min-width="290px"
          >
            <template v-slot:activator="{ on, attrs }">
              <v-text-field
                  v-model="inputBind[achievement.id]"
                  label="New Result"
                  outlined
                  dense
                  rounded
                  readonly
                  v-bind="attrs"
                  v-on="on"
              />
            </template>
            <v-time-picker
                ampm-in-title
                v-if="clockBind[achievement.id]"
                v-model="inputBind[achievement.id]"
                full-width
            />
          </v-menu>
          <v-text-field v-else
                        label="New Result"
                        outlined
                        rounded
                        dense
                        id="resultInput"
                        v-model="inputBind[achievement.id]">
          </v-text-field>
          <v-btn id="resultSaveButton" color="primary" height="40px" class="achievementSaveButton" @click="saveResult(achievement.id)" outlined rounded>Save</v-btn>
        </v-row>
      </v-card>
    </div>
  </v-card>
</template>

<script>
  import {apiActivity} from "../../../api";
  import {mapGetters} from "vuex";
  export default {
    name: "AchievementsCard",
    props: ['achievements', 'snackbar', 'errorMessage', 'loadingResults'],
    data() {
      return {
        latestResult: "0:41",
        clockBind:{},
        inputBind:{},
        results: {},
        snackbarParent: this.snackbar,
        errorMessageParent: this.errorMessage,
        loading: true
      }
    },
    watch: {
      achievements: function() {
        this.loadResults();
      }
    },
    mounted() {
      this.loadResults();
    },
    computed: {
      ...mapGetters(['activity', 'user']),
    },
    methods: {
      /**
       * Loads results for each activity from the database
       */
      loadResults() {
        for(let i = 0; i < this.achievements.length; i++){
          this.results[this.achievements[i].id] = [];
          apiActivity.getResults(this.achievements[i].id).then(response => {
            this.results[this.achievements[i].id] = response.data;
            this.results = {...this.results};
            this.loading = false;
          }).catch(() => {
            this.loading = false;
          });
        }
        if (this.achievements.length === 0) {
          this.loading = false;
        }
      },

      /**
       * Save new result for a given activity into the database
       * @param achievementId
       */
      saveResult(achievementId) {
        if(typeof this.inputBind[achievementId] === 'undefined'){
          this.displayError("Result cannot be empty.");
          return;
        }
        if(this.inputBind[achievementId].trim() == ""){
          this.displayError("Result cannot be empty.");
          return;
        }
        this.loading = true;
        apiActivity.addResult(this.user.profile_id, achievementId, this.inputBind[achievementId])
        .then(() => {
          this.displayError("Result Added");
          this.loading = false;
          this.results[achievementId].push({"achievementId": achievementId, "value": this.inputBind[achievementId]});
          this.inputBind[achievementId] = "";
        }).catch((err) => {
          this.displayError(err);
          this.loading = false;
        })
      },

      /**
       * Display error in snackbar
       * @param msg
       */
      displayError(msg) {
        this.errorMessageParent = msg;
        this.snackbarParent = true;
        this.$emit('update:errorMessage', this.errorMessageParent);
        this.$emit('update:snackbar', this.snackbarParent);
      }
    }
  }
</script>

<style scoped>

</style>