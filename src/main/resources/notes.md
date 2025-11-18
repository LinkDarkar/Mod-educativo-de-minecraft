src/main/resources contains resources such as textures, models, and sounds
it is also the location of fabric.mod.json and any mixin configuration files that your mod uses
assets are stores in a structure that mirrors the structure of resource packs, for example
a texture for a block would be stored inside assets/mod-id/textures/block/block.png


## In relation to the pink_garnet.json file
Here the parent determines that the custom item is going to be rendered in the way normal items
are rendered in the game.

```json
{
    "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "testmod:item/pink_garnet"
  }
}
```

Then we define the texture by indicating that this is
under testmod->textures(this is done with the textures thing right before layer 0)->item->pink_garnet.
And then is going to search for a "pink_garnet.png" file

## In relation to adding blocks
Blocks are a bit more complicated, not only to add them to the game, but also with the extra directories that are required.
Inside "testmod", we have to create a "blockstates" directory. And also a "block" directory in both
"models" and "textures".

Then we will have a "blockstates" .json file, which will determine how the block will look like, or the model it
will refer to inside of the world when we place it down. For the pink_garnet_block, we will create the
pink_garnet_block.json file (the names have to match).

```json
{
  "variants": {
    "": {
      "model": "testmod:block/pink_garnet_block"
    }
  }
}
```

About this, here we will add the variants (for example, wheat has an age variant that changes over time). And because we
don't really need anything else, we just add an empty variant (""), that will just be the "model" that points to
testmod->models->block->pink_garnet_block. This points to the pink_garnet_block.json file that will be there.
And the contents of it will be similar to the ones on items like pink_garnet.json. However, there will be differences.

```json
{
  "parent": "minecraft:block/cube_all",
  "textures": {
    "all": "testmod:block/pink_garnet_block"
  }
}
```

In here we make the parent "minecraft:block/cube_all", which indicates that all the sides of the cube will be the same.
And on textures we indicate that all of them will be "testmod:block/pink_garnet_block", i.e. under
testmod->textures->block->pink_garnet_block.

Now, if we go inside the game right now, what we will see is that the block has a texture inside of the world, but will
not have a proper name nor a texture in the inventory. Remember that when we made the block in "ModBlocks" we also have
to register it as an item. So now we have to do what we have to do with the items too.

So we will create inside of model->item, the pink_garnet_block.json, but unlike with the other items, this one will
just point to the "pink_garnet_block.json" in testmod->block.

```json
{
  "parent": "testmod:block/pink_garnet_block"
}
```

What this will do is point to the block model json file to just show that in the inventory.
Then we just add the name to the en_us.json.

# About crafting
create a data folder inside resources, then inside that, testmod, inside that, recipe, then add the things
however, then one has to add, inside the json, the type, *what is type:"minecraft:crafting_shapeless"*