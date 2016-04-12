﻿using dexter_vs.Analysis.Config;

namespace dexter_vs.UI.Config
{
    /// <summary>
    /// Provides ProjectInfo 
    /// </summary>
    internal interface IProjectInfoProvider
    {
        /// <summary>
        /// Creates new ProjectInfo
        /// </summary>
        /// <returns>new ProjectInfo</returns>
        ProjectInfo Create();
    }
}
