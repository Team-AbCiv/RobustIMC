/**
 * Copyright 2017 3TUSK, et al.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package info.tritusk.robustimc;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;

@Mod(modid = "robustimc", name = "RobustIMC", version = "@VERSION", useMetadata = true)
public enum RobustIMC {

    INSTANCE;

    @Mod.InstanceFactory
    public static RobustIMC getInstance() {
        return INSTANCE;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        File jsonFile = new File(event.getModConfigurationDirectory(), "robustimc.json");
        if (!jsonFile.exists() || !jsonFile.isFile()) {
            return;
        }

        try (FileInputStream json = new FileInputStream(jsonFile)) {
            NBTBase base = JsonToNBT.func_150315_a(IOUtils.toString(json, Charsets.UTF_8));
            if (base instanceof NBTTagList) {
                NBTTagList theTag = (NBTTagList) base;
                for (int index = 0; index < theTag.tagCount(); index++) {
                    NBTTagCompound tag = theTag.getCompoundTagAt(index);
                    final String receiverMod = tag.getString("modid");
                    final String messageKey = tag.getString("key");
                    switch (tag.getString("type").toLowerCase(Locale.ENGLISH)) {
                        case("string"): {
                            FMLInterModComms.sendMessage(receiverMod, messageKey, tag.getString("message"));
                            break;
                        }
                        case("nbt"): {
                            FMLInterModComms.sendMessage(receiverMod, messageKey, tag.getCompoundTag("message"));
                            break;
                        }
                        case("stack"):
                        case("itemstack"): {
                            FMLInterModComms.sendMessage(receiverMod, messageKey, ItemStack.loadItemStackFromNBT(tag.getCompoundTag("message")));
                            break;
                        }
                        default: {
                            FMLLog.warning("[RobustIMC] Yes, one of your input message type is invalid! Double check your json first!");
                            break;
                        }
                    }
                }
            } else {
                FMLLog.severe("[RobustIMC] Well, you should know that you need an array of messages...");
            }
        } catch (Throwable t) {
            FMLLog.getLogger().error("[RobustIMC] RobustIMC encountered with error while resolving IMC message. It will stop being functional.", t);
        }
    }
}
