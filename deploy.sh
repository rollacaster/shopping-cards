mkdir -p classes;
clj -e "(compile 'tech.thomas-sojka.shopping-cards.main)";
npm run css;
shadow-cljs compile app;
clj -M:uberdeps;
scp target/shopping-cards.jar pi@192.168.178.50:/home/pi;
ssh pi@192.168.178.50 sudo systemctl restart shoppinglist.service
