mkdir -p classes;
clj -e "(compile 'tech.thomas-sojka.shopping-cards.main)";
npm run css;
shadow-cljs compile app;
clj -M:uberdeps;
scp target/shopping-cards.jar pi@ubi-hub:/home/pi;
scp /Users/thomas/datomic/dev/shopping-cards/* pi@ubi-hub:/home/pi/datomic/dev/shopping-cards;
ssh pi@ubi-hub sudo systemctl restart shoppinglist.service;
