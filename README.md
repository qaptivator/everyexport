# EveryExport (everyexport)
exports item and recipe data as json.

![everyexport icon, which has the letters EEx in it on a white background, where E's are black and x is cyan (light blue)](./src/main/resources/assets/everyexport/icon.png)

made for enthusiasts, who want to work with actual item data, instead of spending a while trying to find it online.

_**made for fabric 1.20.1**_, newer versions or forge will probably not get supported anytime soon.  
fun fact: this is my first fabric mod. (and i realized that writing in java is so goddamn painful)

# usage

`/everyexport <type>`, where type can be `all`, `items` and `recipes`.  
_note: in future, more export options would get added. if you really need one RIGHT NOW, then feel free to contribute to this project and code it yourself! it's not as hard as you think it is._

it saves every type into its own json file located inside `.minecraft/config/everyexport/<type>.json` (or wherever your minecraft instance is).  
to see the formats used for them, look below.

# data formats

to make things simpler i decided to make the fields be almost identical to the oens found in vanilla datapacks, but there are some inconsistencies.  
i am extremely lazy to write down all of the options right now so just look into the code at `src/main/java/org/qaptivator/everyexport/Everyexport.java` and see it for yourself in methods below.  
_if you're contributing, remember to use snake_case for json fields._

## items.json

## recipes.json

note: some fields have one or multiple something (like ingredients), which in vanilla usually means it's a string or array of strings.  
in my case, it's just an array with one element. deal with it.