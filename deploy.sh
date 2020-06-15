mkdir -p classes;
clj -e "(compile 'tech.thomas-sojka.ingredients.main)";
clj -A:fe-build;
clj -A:uberdeps;
scp target/ingredients.jar pi@192.168.178.50:/home/pi;
ssh pi@192.168.178.50 sudo systemctl restart shoppinglist.service
