mkdir -p classes;
clj -e "(compile 'tech.thomas-sojka.shopping-cards.main)";
npm run css;
shadow-cljs compile app;
clj -M:uberdeps;
scp target/shopping-cards.jar pi@ubi-hub:/home/pi;
ssh pi@ubi-hub sudo systemctl restart shoppinglist.service;
