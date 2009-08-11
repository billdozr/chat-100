(ns chat-100
  (:use compojure clojure.contrib.json.read clojure.contrib.json.write))

(defn now [] (java.util.Date.))
(def date-fmt (java.text.SimpleDateFormat. "hh:mm:ss"))
(def validate-message-list
     (partial every? #(and (:sender %) (:text %) (:create_on %))))

(def messages (ref () :validator validate-message-list))
(def counter (ref 0 :validator number?))
(def guest-counter (ref 0 :validator number?))

(defstruct message :id :sender :text :create_on)
(defn next-count [] (dosync (alter counter inc)))
(defn next-guest-count [] (dosync (alter guest-counter inc)))
(defn guest-account [] (str "Guest " (next-guest-count)))

(defn all-messages-json [] (json-str @messages))
(defn last-message-id [] ((first @messages) :id))

(defn receive-messages-json 
  "Receive all messages for given offset"
  [offset] 
  (json-str (reverse (seq (filter #(> (:id %) offset) @messages)))))

(defn add-message 
  "Add's message to the messages ref"
  [msg] 
  (dosync 
   (commute messages conj msg) (json-str {:last_message_id (last-message-id)})))

(defn layout [title & body]
  (html
    [:head
      [:title title]
      (include-js "http://ajax.googleapis.com/ajax/libs/jquery/1.3/jquery.min.js" "/public/js/chat.js")
      (include-css "/public/css/chat.css")]
    [:body
      [:h2 title]
      body]))

(defn chat-view
  [guest]
  (layout "Chat-100" 
    [:div {:id "chat"}
     [:div {:id "chat_window"}]
     [:input {:id "sender" :name "sender" :type "hidden" :value guest}]
     [:label {:for "id_text"} (str guest ": ")][:input {:id "text" :name "text" :type "text"}] 
     [:input {:id "send_button" :type "button" :value "send"}]]))

(defn add-message-view
  [sender text]
  (add-message (struct message (next-count) sender text (.format date-fmt (now)))))

(defn login-view
  [session next-url]
  (layout "Chat-100: Choose Nick"
   (form-to [:post "/login"]
     [:input {:id "next-url" :name "next-url" :type "hidden" :value next-url}]
     [:input {:id "nickname" :name "nickname" :maxlength 14}]
     (submit-button "join chat"))))

(defn get-next-url [params] 
  (if (= (params :next-url) "") (str "/chat") (params :next-url)))

(defn login-controller
  [params session]
  (if (= (.trim (params :nickname)) "")
    [(session-assoc :login (guest-account)) (redirect-to (get-next-url params))]
  [(session-assoc :login (params :nickname)) (redirect-to (get-next-url params))]))

(defn chat-controller
  [session]
  (let [guest (session :login)]
    (if guest
      (chat-view guest)
    (redirect-to "/login?next-url=/chat"))))

(defroutes chatservice
  (GET "/login" (login-view session (params :next-url)))
  (POST "/login" (login-controller params session))
  (POST "/chat/add-message" [{:headers {"Content-Type" "application/json"}}] 
       (add-message-view (params :sender) (params :text)))
  (GET "/chat/receive-messages/:id" [{:headers {"Content-Type" "application/json"}}] 
       (receive-messages-json (Integer. (params :id))))
  (GET "/chat" (chat-controller session))
  (GET "/*" 
    (or (serve-file "~/shared/static" (params :*)) :next)) 
  (ANY "*" 
    (page-not-found)))

(decorate chatservice       
          (with-session {:type :memory, :expires 600}))

(defserver chat-server {:port 8080}
  "/*" (servlet chatservice))

(start chat-server)