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

import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;

@Mod(modid = "robustimc", name = "RobustIMC", version = "@VERSION@", useMetadata = true)
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
            NBTTagCompound tag = JsonToNBT.getTagFromJson(IOUtils.toString(json, Charsets.UTF_8));
            tag.getKeySet().forEach(key -> {
                NBTTagCompound message = tag.getCompoundTag(key);
                if (message.getSize() == 0) {
                    FMLLog.warning("[RobustIMC] Message \"{}\" is either empty or invalid! This is user's error!", key);
                    return;
                }
                final String receiverMod = message.getString("modid");
                final String messageKey = message.getString("key");
                if (receiverMod.isEmpty() || messageKey.isEmpty()) {
                    FMLLog.warning("[RobustIMC] Message \"{}\" has invalid receiver or message key! This is user's error!", key);
                    return;
                }
                switch (message.getString("type").toLowerCase(Locale.ENGLISH)) {
                    case ("string"): {
                        FMLInterModComms.sendMessage(receiverMod, messageKey, message.getString("message"));
                        break;
                    }
                    case ("nbt"): {
                        FMLInterModComms.sendMessage(receiverMod, messageKey, message.getCompoundTag("message"));
                        break;
                    }
                    case ("item"):
                    case ("stack"):
                    case ("itemstack"): {
                        FMLInterModComms.sendMessage(receiverMod, messageKey, ItemStack.loadItemStackFromNBT(message.getCompoundTag("message")));
                        break;
                    }
                    case ("rs"):
                    case ("resourcelocation"): {
                        NBTTagCompound content = message.getCompoundTag("message");
                        FMLInterModComms.sendMessage(receiverMod, messageKey, new ResourceLocation(content.getString("domain"), content.getString("path")));
                        break;
                    }
                    default: {
                        FMLLog.warning("[RobustIMC] Message \"{}\" has invalid message type! This is user's error!", key);
                        break;
                    }
                }
            });
        } catch (Throwable t) {
            FMLLog.getLogger().error("[RobustIMC] RobustIMC encountered with error while resolving IMC message. It will stop being functional.", t);
        }
    }
}
