﻿using dexter_vs.Analysis.Config;

namespace dexter_vs.UI.Config
{
    /// <summary>
    /// Provides DexterInfo 
    /// </summary>
    internal interface IDexterInfoProvider
    {
        /// <summary>
        /// Creates new DexterInfo
        /// </summary>
        /// <returns>new DexterInfo</returns>
        DexterInfo Create();
    }
}
