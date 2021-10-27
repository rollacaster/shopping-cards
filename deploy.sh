mkdir -p classes;
clj -e "(compile 'tech.thomas-sojka.shopping-cards.main)";
npm run css;
shadow-cljs compile app;
clj -M:uberdeps;
scp target/shopping-cards.jar pi@192.168.178.50:/home/pi;
scp /Users/thomas/datomic/dev/shopping-cards/* pi@192.168.178.50:/home/pi/datomic/dev/shopping-cards;
ssh pi@192.168.178.50 sudo systemctl restart shoppinglist.service;
